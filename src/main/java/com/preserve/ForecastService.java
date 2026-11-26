package com.preserve;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.preserve.Util.*;

/**
 * Demand forecasting + waste-risk engine (pure Java ML, no external service).
 * Model: exponential-smoothing level + damped trend, learned day-of-week factors,
 * learned promotion uplift, category seasonal index, last-year (YoY) adjustment,
 * residual-variance based probability of leftover stock, FEFO batch simulation.
 * Discount elasticity is learned from completed interventions in the database.
 */
@Service
public class ForecastService {
  record Sale(LocalDate d, int units, int promo) {}

  static final int H = 120, LEAD = 3;
  static final double[] GENERIC_DOW = {1.0, 0.95, 0.95, 1.0, 1.05, 1.2, 1.25};
  static final Map<String, Double> KG = Map.of("Biscuits", 0.1, "Snacks", 0.08, "Cereals", 0.5, "Dairy", 0.5,
      "Beverages", 0.75, "Sauces", 0.5, "Instant Food", 0.07, "Breakfast", 1.0, "Spreads", 0.35, "Staples", 2.5);

  private final JdbcTemplate jdbc;
  private final Map<String, double[]> season = new LinkedHashMap<>();

  public ForecastService(JdbcTemplate jdbc) throws IOException {
    this.jdbc = jdbc;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new ClassPathResource("data/seasonality.csv").getInputStream(), StandardCharsets.UTF_8))) {
      br.readLine();
      String l;
      while ((l = br.readLine()) != null) {
        String[] p = l.split(",");
        double[] a = new double[12];
        for (int i = 0; i < 12; i++) a[i] = Double.parseDouble(p[i + 1]);
        season.put(p[0], a);
      }
    }
  }

  public Map<String, double[]> seasonMap() { return season; }
  public double idx(String cat, int month) { double[] a = season.get(cat); return a == null ? 1.0 : a[month - 1]; }
  public static double kg(String cat) { return KG.getOrDefault(cat, 0.5); }

  static double phi(double z) {
    double t = 1 / (1 + 0.2316419 * Math.abs(z)), d = 0.3989423 * Math.exp(-z * z / 2);
    double p = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))));
    return z > 0 ? 1 - p : p;
  }

  /** Learns discount elasticity and visibility uplift from completed interventions. */
  public double[] learn() {
    double e = 0.0185, v = 0.15;
    List<Double> es = new ArrayList<>(), vs = new ArrayList<>();
    for (Map<String, Object> r : rows(jdbc, "SELECT type, discount_pct, units_sold, baseline_daily, start_date, completed_date FROM interventions WHERE status='COMPLETED' AND baseline_daily>0 AND completed_date IS NOT NULL")) {
      long days = ChronoUnit.DAYS.between(ld(r.get("start_date")), ld(r.get("completed_date")));
      if (days < 3) continue;
      double ratio = num(r.get("units_sold")) / (num(r.get("baseline_daily")) * days);
      String t = (String) r.get("type");
      double d = num(r.get("discount_pct"));
      if ((t.equals("DISCOUNT") || t.equals("PROMOTION")) && d > 0) es.add((ratio - 1) / d);
      else if (t.equals("VISIBILITY")) vs.add(ratio - 1);
    }
    if (!es.isEmpty()) e = clamp(es.stream().mapToDouble(x -> x).average().orElse(e), 0.008, 0.05);
    if (!vs.isEmpty()) v = clamp(vs.stream().mapToDouble(x -> x).average().orElse(v), 0.05, 0.4);
    return new double[]{e, v};
  }

  static Map<String, Object> rec(String code, Object... kv) {
    Map<String, Object> params = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) params.put((String) kv[i], kv[i + 1]);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("code", code);
    m.put("params", params);
    return m;
  }

  public Map<String, Object> analyze(Map<String, Object> p) {
    long pid = lng(p.get("id"));
    String cat = (String) p.get("category");
    double price = num(p.get("price"));
    LocalDate today = LocalDate.now();

    List<Sale> hist = jdbc.query("SELECT sale_date, units, promo FROM sales WHERE product_id=? ORDER BY sale_date",
        (rs, i) -> new Sale(rs.getDate(1).toLocalDate(), rs.getInt(2), rs.getInt(3)), pid);
    List<Map<String, Object>> batches = rows(jdbc,
        "SELECT id, remaining, expiry_date FROM stock_batches WHERE product_id=? AND remaining>0 ORDER BY expiry_date", pid);
    int n = hist.size();

    // ---------- 1. learn demand pattern ----------
    double[] dow = GENERIC_DOW.clone();
    double uplift = 1, level, sigmaX, g = 0;
    String basis;
    if (n >= 7) {
      basis = "HISTORY";
      double sp = 0, sn = 0;
      int cp = 0, cn = 0;
      for (Sale s : hist) { if (s.promo() == 1) { sp += s.units(); cp++; } else { sn += s.units(); cn++; } }
      if (cp >= 5 && cn >= 5 && sn > 0) uplift = clamp((sp / cp) / (sn / cn), 1.0, 1.5);
      if (n >= 28) {
        double[] sm = new double[7];
        int[] c = new int[7];
        double tot = 0;
        for (Sale s : hist) {
          double u = s.units() / (s.promo() == 1 ? uplift : 1);
          int k = s.d().getDayOfWeek().getValue() - 1;
          sm[k] += u; c[k]++; tot += u;
        }
        double mean = tot / n;
        for (int k = 0; k < 7; k++) if (c[k] > 0 && mean > 0) dow[k] = clamp(sm[k] / c[k] / mean, 0.6, 1.6);
      }
      double[] x = new double[n];
      for (int i = 0; i < n; i++) {
        Sale s = hist.get(i);
        x[i] = s.units() / ((s.promo() == 1 ? uplift : 1) * dow[s.d().getDayOfWeek().getValue() - 1] * idx(cat, s.d().getMonthValue()));
      }
      double init = 0;
      for (int i = 0; i < 7; i++) init += x[i];
      level = init / 7;
      for (int i = 0; i < n; i++) level = 0.15 * x[i] + 0.85 * level;
      double mu = Arrays.stream(x).average().orElse(0), var = 0;
      for (double v : x) var += (v - mu) * (v - mu);
      sigmaX = Math.sqrt(var / Math.max(1, n - 1));
      double a7 = 0;
      for (int i = n - 7; i < n; i++) a7 += x[i];
      a7 /= 7;
      if (level > 0) g = clamp(a7 / level - 1, -0.15, 0.15);
      sigmaX = Math.max(sigmaX, 0.15 * level);
    } else {
      double est = num(p.get("est_daily"));
      if (est > 0) { basis = "ESTIMATE"; level = est; }
      else {
        Double ca = jdbc.queryForObject("SELECT AVG(s.units) FROM sales s JOIN products q ON q.id=s.product_id WHERE q.category=? AND q.shop_id=?",
            Double.class, cat, lng(p.get("shop_id")));
        if (ca != null && ca > 0) { basis = "CATEGORY"; level = ca; } else { basis = "DEFAULT"; level = 5; }
      }
      level = level / idx(cat, today.getMonthValue());
      sigmaX = 0.35 * level;
    }

    // ---------- 2. last-year (YoY) seasonal adjustment ----------
    double lyAdj = 1;
    Map<String, Object> ly = new LinkedHashMap<>();
    ly.put("available", false);
    LocalDate b0 = today.minusYears(1);
    double ps = 0, ns = 0;
    int pc = 0, nc = 0;
    for (Sale s : hist) {
      long off = ChronoUnit.DAYS.between(b0, s.d());
      if (off >= -28 && off < 0) { ps += s.units(); pc++; } else if (off >= 0 && off < 28) { ns += s.units(); nc++; }
    }
    if (pc >= 10 && nc >= 10 && ps > 0) {
      double pr = 0, nr = 0;
      for (int i = 1; i <= 28; i++) { pr += idx(cat, today.minusDays(i).getMonthValue()); nr += idx(cat, today.plusDays(i - 1).getMonthValue()); }
      lyAdj = clamp(Math.sqrt(((ns / nc) / (ps / pc)) / (nr / pr)), 0.7, 1.4);
      ly.put("available", true);
      ly.put("prevAvg", r1(ps / pc));
      ly.put("nextAvg", r1(ns / nc));
    }

    // ---------- 3. forecast next H days ----------
    double[] mean = new double[H], cum = new double[H + 1], vcum = new double[H + 1];
    for (int i = 0; i < H; i++) {
      LocalDate d = today.plusDays(i);
      double f = dow[d.getDayOfWeek().getValue() - 1] * idx(cat, d.getMonthValue()) * lyAdj;
      mean[i] = Math.max(0, level * (1 + g * Math.min(i + 1, 14) / 14.0) * f);
      cum[i + 1] = cum[i] + mean[i];
      double sd = sigmaX * f;
      vcum[i + 1] = vcum[i] + sd * sd;
    }

    // ---------- 4. FEFO batch simulation: how much will be left when each batch expires? ----------
    int stock = 0;
    for (Map<String, Object> b : batches) stock += (int) lng(b.get("remaining"));
    double consumed = 0, leftover = 0, prob = 0;
    List<Map<String, Object>> bl = new ArrayList<>();
    for (Map<String, Object> b : batches) {
      int rem = (int) lng(b.get("remaining"));
      LocalDate ex = ld(b.get("expiry_date"));
      long dl = ChronoUnit.DAYS.between(today, ex);
      int days = (int) Math.max(0, Math.min(H, dl));
      double avail = Math.max(0, cum[days] - consumed);
      double lo = Math.max(0, rem - avail);
      double pb = days == 0 ? 1.0 : phi((rem - avail) / Math.max(Math.sqrt(vcum[days]), 1e-6));
      leftover += lo;
      consumed += Math.min(rem, avail);
      prob = Math.max(prob, pb);
      Map<String, Object> bm = new LinkedHashMap<>();
      bm.put("id", b.get("id")); bm.put("remaining", rem); bm.put("expiry", ex.toString());
      bm.put("daysLeft", dl); bm.put("predictedLeftover", Math.round(lo));
      bl.add(bm);
    }
    int so = -1;
    for (int k = 1; k <= H; k++) if (cum[k] >= stock) { so = k; break; }
    if (stock == 0) so = 0;
    double pct = stock > 0 ? leftover / stock : 0;
    String risk = stock == 0 ? "NONE" : pct >= 0.6 ? "VERY_HIGH" : pct >= 0.35 ? "HIGH" : pct >= 0.1 ? "MEDIUM" : "LOW";

    // ---------- 5. recommendations ----------
    double[] lv = learn();
    double e = lv[0];
    List<Map<String, Object>> recs = new ArrayList<>();
    int suggestedDiscount = 0;
    if (stock == 0) {
      recs.add(rec("REORDER_NOW", "qty", (int) Math.ceil(cum[14]), "priority", "URGENT"));
    } else if (leftover >= 1 && !risk.equals("LOW")) {
      double sold = Math.max(1, stock - leftover);
      double need = (stock / sold - 1) / e;
      suggestedDiscount = (int) (Math.round(clamp(need, 5, 40) / 5.0) * 5);
      int saved = (int) Math.round(Math.min(leftover, sold * e * suggestedDiscount));
      recs.add(rec("DISCOUNT", "pct", suggestedDiscount, "saved", saved));
      if (!risk.equals("MEDIUM")) recs.add(rec("BUNDLE"));
      recs.add(rec("VISIBILITY", "pct", Math.round(lv[1] * 100)));
      if (risk.equals("VERY_HIGH") || need > 40) recs.add(rec("TRANSFER", "units", Math.round(leftover * 0.5)));
    } else if (so > 0 && so <= 7) {
      recs.add(rec("REORDER", "days", so, "qty", (int) Math.ceil(cum[14]),
          "by", today.plusDays(Math.max(0, so - LEAD)).toString(), "priority", so <= 3 ? "URGENT" : "SOON"));
    } else {
      recs.add(rec("NORMAL"));
    }
    double sr = idx(cat, today.plusMonths(1).getMonthValue()) / idx(cat, today.getMonthValue());
    if (sr >= 1.12) recs.add(rec("SEASON_UP", "pct", Math.round((sr - 1) * 100)));
    else if (sr <= 0.88) recs.add(rec("SEASON_DOWN", "pct", Math.round((1 - sr) * 100)));
    if (!basis.equals("HISTORY")) recs.add(rec("NEW_BASIS", "basis", basis));

    // ---------- 6. response ----------
    Map<String, Object> o = new LinkedHashMap<>();
    o.put("id", pid); o.put("name", p.get("name")); o.put("category", cat); o.put("price", price);
    o.put("shopId", p.get("shop_id")); o.put("stock", stock);
    o.put("daysToExpiry", batches.isEmpty() ? null : ((Number) bl.get(0).get("daysLeft")).longValue());
    o.put("expectedSales", Math.round(stock - leftover));
    o.put("leftover", Math.round(leftover));
    o.put("leftoverPct", Math.round(pct * 100));
    o.put("wasteProb", stock == 0 ? 0 : Math.round(prob * 100));
    o.put("riskScore", Math.round(pct * 100));
    o.put("risk", risk);
    o.put("sellOutDays", so);
    o.put("sellOutDate", so > 0 ? today.plusDays(so - 1).toString() : null);
    o.put("next3", new double[]{r1(mean[0]), r1(mean[1]), r1(mean[2])});
    double a = 0;
    for (int i = 0; i < 7; i++) a += mean[i];
    o.put("avgDaily", r1(a / 7));
    o.put("basis", basis);
    o.put("trend", g > 0.03 ? "UP" : g < -0.03 ? "DOWN" : "STABLE");
    o.put("promoUplift", r1(uplift * 100) / 100.0);
    o.put("seasonIdx", season.getOrDefault(cat, new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}));
    o.put("lastYear", ly);
    o.put("valueAtRisk", Math.round(leftover * price));
    o.put("suggestedDiscount", suggestedDiscount);
    o.put("needsReorder", recs.stream().anyMatch(r -> r.get("code").toString().startsWith("REORDER")));
    o.put("recs", recs);
    o.put("batches", bl);

    List<Map<String, Object>> fc = new ArrayList<>();
    for (int i = 0; i < 21; i++) { Map<String, Object> m = new LinkedHashMap<>(); m.put("date", today.plusDays(i).toString()); m.put("units", r1(mean[i])); fc.add(m); }
    o.put("forecast", fc);
    List<Map<String, Object>> hs = new ArrayList<>();
    for (int i = Math.max(0, n - 30); i < n; i++) { Map<String, Object> m = new LinkedHashMap<>(); m.put("date", hist.get(i).d().toString()); m.put("units", hist.get(i).units()); hs.add(m); }
    o.put("history", hs);
    Map<String, double[]> mo = new TreeMap<>();
    for (Sale s : hist) { double[] t = mo.computeIfAbsent(s.d().toString().substring(0, 7), k -> new double[2]); t[0] += s.units(); t[1]++; }
    List<Map<String, Object>> ml = new ArrayList<>();
    mo.forEach((k, v) -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("month", k); m.put("units", Math.round(v[0])); m.put("avgDaily", r1(v[0] / v[1])); ml.add(m); });
    o.put("monthly", ml);
    return o;
  }
}
