package com.preserve;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.preserve.Util.*;

/**
 * First start only: loads the supplied Excel dataset (converted to CSV in resources/data) into the database.
 * Sales dates are shifted so the newest sale is "yesterday" and expiry = today + Days_Remaining,
 * so the demo always looks live. Demo login: demo@preserve.com / demo123
 */
@Component
public class DataLoader implements CommandLineRunner {
  private final JdbcTemplate jdbc;
  DataLoader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  static List<String[]> csv(String path) throws IOException {
    List<String[]> out = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
      br.readLine();
      String l;
      while ((l = br.readLine()) != null) if (!l.isBlank()) out.add(l.split(","));
    }
    return out;
  }

  @Override
  public void run(String... args) throws Exception {
    Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    if (users != null && users > 0) return;
    LocalDate today = LocalDate.now();
    long uid = insert(jdbc, "INSERT INTO users(name,email,password_hash) VALUES(?,?,?)",
        "Demo Owner", "demo@preserve.com", new BCryptPasswordEncoder().encode("demo123"));
    long shop = insert(jdbc, "INSERT INTO shops(code,name,city,owner_id) VALUES(?,?,?,?)", "S005", "Demo Supermart", "Faridabad", uid);

    Map<String, Long> pid = new HashMap<>();
    Map<String, String> cat = new HashMap<>();
    Map<String, Double> prc = new HashMap<>();
    for (String[] r : csv("data/inventory.csv")) {
      long id = insert(jdbc, "INSERT INTO products(code,name,category,price,shop_id) VALUES(?,?,?,?,?)", r[0], r[1], r[2], Double.parseDouble(r[3]), shop);
      pid.put(r[0], id); cat.put(r[0], r[2]); prc.put(r[0], Double.parseDouble(r[3]));
      int qty = Integer.parseInt(r[4]);
      jdbc.update("INSERT INTO stock_batches(product_id,quantity,remaining,added_on,expiry_date) VALUES(?,?,?,?,?)",
          id, qty, qty, Date.valueOf(today), Date.valueOf(today.plusDays(Integer.parseInt(r[5]))));
    }
    List<String[]> sales = csv("data/sales.csv");
    LocalDate max = LocalDate.MIN;
    for (String[] r : sales) { LocalDate d = LocalDate.parse(r[0]); if (d.isAfter(max)) max = d; }
    long shift = ChronoUnit.DAYS.between(max, today.minusDays(1));
    Map<String, double[]> tot = new HashMap<>();
    for (String[] r : sales) {
      jdbc.update("INSERT INTO sales(product_id,sale_date,units,price,promo) VALUES(?,?,?,?,?)", pid.get(r[1]),
          Date.valueOf(LocalDate.parse(r[0]).plusDays(shift)), Integer.parseInt(r[2]), Double.parseDouble(r[3]), Integer.parseInt(r[4]));
      double[] t = tot.computeIfAbsent(r[1], k -> new double[2]);
      t[0] += Integer.parseInt(r[2]); t[1]++;
    }
    // Past interventions from the dataset (completed, 7 days long) -> used to learn discount elasticity
    for (String[] r : csv("data/outcomes.csv")) {
      String code = r[0];
      String type = r[1].toLowerCase().contains("markdown") ? "DISCOUNT" : "VISIBILITY";
      int disc = (int) Double.parseDouble(r[2]), before = Integer.parseInt(r[3]), sold = Integer.parseInt(r[4]);
      double[] t = tot.get(code);
      double base = t == null ? 0 : t[0] / t[1];
      int saved = (int) Math.max(0, Math.round(sold - base * 7));
      double kg = saved * ForecastService.kg(cat.get(code));
      double val = saved * prc.get(code) * (1 - disc / 100.0);
      jdbc.update("INSERT INTO interventions(product_id,shop_id,type,discount_pct,start_date,stock_before,predicted_leftover,baseline_daily,status,units_sold,units_saved,kg_saved,value_recovered,completed_date) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
          pid.get(code), shop, type, disc, Date.valueOf(today.minusDays(8)), before, Integer.parseInt(r[5]), base, "COMPLETED", sold, saved,
          Math.round(kg * 10) / 10.0, (double) Math.round(val), Date.valueOf(today.minusDays(1)));
    }
    System.out.println(">>> PreServe demo data loaded. Login: demo@preserve.com / demo123");
  }
}
