package com.preserve;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.preserve.Util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final JdbcTemplate jdbc;
  private final ForecastService fs;
  private final Auth auth;
  private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
  static final Set<String> TYPES = Set.of("DISCOUNT", "PROMOTION", "BUNDLE", "VISIBILITY", "TRANSFER");

  public ApiController(JdbcTemplate jdbc, ForecastService fs, Auth auth) { this.jdbc = jdbc; this.fs = fs; this.auth = auth; }

  // ---------------------------------------------------------------- helpers
  static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
  static String str(Object o) { return o == null ? "" : o.toString().trim(); }

  private void own(long uid, long shopId) {
    Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM shops WHERE id=? AND owner_id=?", Integer.class, shopId, uid);
    if (c == null || c == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This shop is not yours");
  }

  private Map<String, Object> product(long uid, long pid) {
    List<Map<String, Object>> l = rows(jdbc, "SELECT p.* FROM products p JOIN shops s ON s.id=p.shop_id WHERE p.id=? AND s.owner_id=?", pid, uid);
    if (l.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    return l.get(0);
  }

  private List<Map<String, Object>> shopsOf(long uid) {
    return rows(jdbc, "SELECT id, code, name, city FROM shops WHERE owner_id=? ORDER BY id", uid);
  }

  private long createShop(long uid, String name, String city) {
    long id = insert(jdbc, "INSERT INTO shops(code,name,city,owner_id) VALUES(?,?,?,?)", "TMP", name, city, uid);
    jdbc.update("UPDATE shops SET code=? WHERE id=?", "SHOP" + (1000 + id), id);
    return id;
  }

  private long addStock(long shopId, String name, String cat, double price, int qty, LocalDate expiry, Double est) {
    if (name.isEmpty() || cat.isEmpty() || qty <= 0 || price < 0) throw bad("Name, category, price and quantity are required");
    if (!expiry.isAfter(LocalDate.now().minusDays(1))) throw bad("Expiry date must be today or later");
    List<Map<String, Object>> ex = rows(jdbc, "SELECT id FROM products WHERE shop_id=? AND LOWER(name)=LOWER(?)", shopId, name);
    long pid;
    if (ex.isEmpty()) {
      pid = insert(jdbc, "INSERT INTO products(code,name,category,price,shop_id,est_daily) VALUES(?,?,?,?,?,?)", "TMP", name, cat, price, shopId, est);
      jdbc.update("UPDATE products SET code=? WHERE id=?", "P" + String.format("%04d", pid), pid);
    } else {
      pid = lng(ex.get(0).get("id"));
      jdbc.update("UPDATE products SET price=? WHERE id=?", price, pid);
      if (est != null && est > 0) jdbc.update("UPDATE products SET est_daily=? WHERE id=?", est, pid);
    }
    jdbc.update("INSERT INTO stock_batches(product_id,quantity,remaining,added_on,expiry_date) VALUES(?,?,?,?,?)",
        pid, qty, qty, Date.valueOf(LocalDate.now()), Date.valueOf(expiry));
    return pid;
  }

  // ---------------------------------------------------------------- auth
  @PostMapping("/auth/register")
  public Map<String, Object> register(@RequestBody Map<String, Object> b) {
    String name = str(b.get("name")), email = str(b.get("email")).toLowerCase(), pw = str(b.get("password"));
    String shop = str(b.get("shopName")), city = str(b.get("city"));
    if (name.isEmpty() || !email.contains("@") || pw.length() < 6 || shop.isEmpty()) throw bad("Fill all fields (password min 6 characters)");
    if (!rows(jdbc, "SELECT id FROM users WHERE email=?", email).isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    long uid = insert(jdbc, "INSERT INTO users(name,email,password_hash) VALUES(?,?,?)", name, email, enc.encode(pw));
    createShop(uid, shop, city);
    return session(uid, name);
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody Map<String, Object> b) {
    List<Map<String, Object>> u = rows(jdbc, "SELECT id, name, password_hash FROM users WHERE email=?", str(b.get("email")).toLowerCase());
    if (u.isEmpty() || !enc.matches(str(b.get("password")), (String) u.get(0).get("password_hash")))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
    return session(lng(u.get(0).get("id")), (String) u.get(0).get("name"));
  }

  private Map<String, Object> session(long uid, String name) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("token", auth.issue(uid)); m.put("name", name); m.put("shops", shopsOf(uid));
    return m;
  }

  @PostMapping("/logout")
  public Map<String, Object> logout(HttpServletRequest r) {
    String a = r.getHeader("Authorization");
    if (a != null && a.startsWith("Bearer ")) auth.revoke(a.substring(7));
    return Map.of("ok", true);
  }

  // ---------------------------------------------------------------- shops
  @GetMapping("/shops")
  public List<Map<String, Object>> shops(@RequestAttribute("uid") long uid) { return shopsOf(uid); }

  @PostMapping("/shops")
  public List<Map<String, Object>> addShop(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    if (str(b.get("name")).isEmpty()) throw bad("Shop name required");
    createShop(uid, str(b.get("name")), str(b.get("city")));
    return shopsOf(uid);
  }

  // ---------------------------------------------------------------- products / stock
  @GetMapping("/inventory")
  public List<Map<String, Object>> inventory(@RequestAttribute("uid") long uid, @RequestParam long shopId) {
    own(uid, shopId);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> p : rows(jdbc, "SELECT * FROM products WHERE shop_id=? ORDER BY name", shopId)) {
      Map<String, Object> a = fs.analyze(p);
      a.keySet().removeAll(List.of("forecast", "history", "monthly", "batches", "seasonIdx", "lastYear"));
      out.add(a);
    }
    return out;
  }

  @GetMapping("/products/{id}")
  public Map<String, Object> analysis(@RequestAttribute("uid") long uid, @PathVariable long id) {
    return fs.analyze(product(uid, id));
  }

  /** Add a NEW product (or new stock of an existing product name) and return the AI evaluation immediately. */
  @PostMapping("/products")
  public Map<String, Object> addProduct(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    long shopId = lng(b.get("shopId"));
    own(uid, shopId);
    Double est = str(b.get("estDaily")).isEmpty() ? null : num(b.get("estDaily"));
    long pid = addStock(shopId, str(b.get("name")), str(b.get("category")), num(b.get("price")), (int) num(b.get("quantity")),
        LocalDate.parse(str(b.get("expiryDate"))), est);
    return fs.analyze(product(uid, pid));
  }

  @PostMapping("/stock")
  public Map<String, Object> addStockBatch(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    Map<String, Object> p = product(uid, lng(b.get("productId")));
    addStock(lng(p.get("shop_id")), (String) p.get("name"), (String) p.get("category"), num(p.get("price")),
        (int) num(b.get("quantity")), LocalDate.parse(str(b.get("expiryDate"))), null);
    return fs.analyze(product(uid, lng(p.get("id"))));
  }

  /** CSV columns: name,category,price,quantity,days_remaining[,est_daily] */
  @PostMapping("/products/import")
  public Map<String, Object> importCsv(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    long shopId = lng(b.get("shopId"));
    own(uid, shopId);
    int ok = 0;
    List<String> errors = new ArrayList<>();
    String[] lines = str(b.get("csv")).split("\\r?\\n");
    for (int i = 1; i < lines.length; i++) {
      if (lines[i].isBlank()) continue;
      try {
        String[] c = lines[i].split(",");
        addStock(shopId, c[0].trim(), c[1].trim(), Double.parseDouble(c[2].trim()), Integer.parseInt(c[3].trim()),
            LocalDate.now().plusDays(Integer.parseInt(c[4].trim())), c.length > 5 && !c[5].isBlank() ? Double.parseDouble(c[5].trim()) : null);
        ok++;
      } catch (Exception e) { errors.add("Line " + (i + 1) + ": " + e.getMessage()); }
    }
    return Map.of("imported", ok, "errors", errors);
  }

  @PostMapping("/sales")
  public Map<String, Object> sale(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    Map<String, Object> p = product(uid, lng(b.get("productId")));
    long pid = lng(p.get("id"));
    int units = (int) num(b.get("units"));
    if (units <= 0) throw bad("Units must be positive");
    Integer stock = jdbc.queryForObject("SELECT COALESCE(SUM(remaining),0) FROM stock_batches WHERE product_id=?", Integer.class, pid);
    if (stock == null || stock < units) throw bad("Only " + stock + " units in stock");
    LocalDate d = str(b.get("date")).isEmpty() ? LocalDate.now() : LocalDate.parse(str(b.get("date")));
    int need = units;
    for (Map<String, Object> bt : rows(jdbc, "SELECT id, remaining FROM stock_batches WHERE product_id=? AND remaining>0 ORDER BY expiry_date", pid)) {
      if (need <= 0) break;
      int take = Math.min(need, (int) lng(bt.get("remaining")));
      jdbc.update("UPDATE stock_batches SET remaining=remaining-? WHERE id=?", take, bt.get("id"));
      need -= take;
    }
    boolean promo = Boolean.TRUE.equals(b.get("promo")) || "true".equals(str(b.get("promo")));
    jdbc.update("INSERT INTO sales(product_id,sale_date,units,price,promo) VALUES(?,?,?,?,?)", pid, Date.valueOf(d), units, num(p.get("price")), promo ? 1 : 0);
    return fs.analyze(product(uid, pid));
  }

  // ---------------------------------------------------------------- dashboard
  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(@RequestAttribute("uid") long uid, @RequestParam long shopId) {
    List<Map<String, Object>> inv = inventory(uid, shopId);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String k : List.of("LOW", "MEDIUM", "HIGH", "VERY_HIGH", "NONE")) counts.put(k, 0);
    long units = 0, value = 0, atRiskValue = 0;
    for (Map<String, Object> a : inv) {
      counts.merge((String) a.get("risk"), 1, Integer::sum);
      units += lng(a.get("stock"));
      value += Math.round(lng(a.get("stock")) * num(a.get("price")));
      atRiskValue += lng(a.get("valueAtRisk"));
    }
    List<Map<String, Object>> top = new ArrayList<>(inv);
    top.sort((x, y) -> Double.compare(num(y.get("valueAtRisk")), num(x.get("valueAtRisk"))));
    top.removeIf(a -> num(a.get("leftover")) < 1);
    List<Map<String, Object>> reorder = new ArrayList<>();
    for (Map<String, Object> a : inv) if (Boolean.TRUE.equals(a.get("needsReorder"))) reorder.add(a);
    Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM interventions WHERE shop_id=? AND status='ACTIVE'", Integer.class, shopId);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("products", inv.size()); m.put("stockUnits", units); m.put("stockValue", value); m.put("valueAtRisk", atRiskValue);
    m.put("counts", counts); m.put("atRisk", counts.get("HIGH") + counts.get("VERY_HIGH"));
    m.put("top", top.subList(0, Math.min(6, top.size()))); m.put("reorder", reorder);
    m.put("activeActions", active); m.put("impact", impact(shopId));
    return m;
  }

  // ---------------------------------------------------------------- interventions (action tracking)
  @GetMapping("/interventions")
  public List<Map<String, Object>> interventions(@RequestAttribute("uid") long uid, @RequestParam long shopId) {
    own(uid, shopId);
    return rows(jdbc, "SELECT i.*, p.name AS product_name FROM interventions i JOIN products p ON p.id=i.product_id WHERE i.shop_id=? ORDER BY i.id DESC", shopId);
  }

  @PostMapping("/interventions")
  public Map<String, Object> startIntervention(@RequestAttribute("uid") long uid, @RequestBody Map<String, Object> b) {
    Map<String, Object> p = product(uid, lng(b.get("productId")));
    String type = str(b.get("type")).toUpperCase();
    if (!TYPES.contains(type)) throw bad("Unknown action type");
    Map<String, Object> a = fs.analyze(p);
    long id = insert(jdbc, "INSERT INTO interventions(product_id,shop_id,type,discount_pct,start_date,stock_before,predicted_leftover,baseline_daily,status) VALUES(?,?,?,?,?,?,?,?,?)",
        p.get("id"), p.get("shop_id"), type, (int) num(b.get("discountPct")), Date.valueOf(LocalDate.now()),
        a.get("stock"), a.get("leftover"), a.get("avgDaily"), "ACTIVE");
    return Map.of("id", id);
  }

  @PostMapping("/interventions/{id}/complete")
  public Map<String, Object> complete(@RequestAttribute("uid") long uid, @PathVariable long id, @RequestBody(required = false) Map<String, Object> b) {
    List<Map<String, Object>> l = rows(jdbc, "SELECT i.*, p.category, p.price FROM interventions i JOIN products p ON p.id=i.product_id JOIN shops s ON s.id=i.shop_id WHERE i.id=? AND s.owner_id=?", id, uid);
    if (l.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found");
    Map<String, Object> i = l.get(0);
    if (!"ACTIVE".equals(i.get("status"))) throw bad("Already completed");
    LocalDate start = ld(i.get("start_date"));
    long days = Math.max(1, ChronoUnit.DAYS.between(start, LocalDate.now()));
    int sold;
    if (b != null && !str(b.get("unitsSold")).isEmpty()) sold = (int) num(b.get("unitsSold"));
    else {
      Integer s = jdbc.queryForObject("SELECT COALESCE(SUM(units),0) FROM sales WHERE product_id=? AND sale_date>=?", Integer.class, i.get("product_id"), Date.valueOf(start));
      sold = s == null ? 0 : s;
    }
    int saved = (int) Math.max(0, Math.round(sold - num(i.get("baseline_daily")) * days));
    double kg = Math.round(saved * ForecastService.kg((String) i.get("category")) * 10) / 10.0;
    double val = Math.round(saved * num(i.get("price")) * (1 - num(i.get("discount_pct")) / 100.0));
    jdbc.update("UPDATE interventions SET status='COMPLETED', units_sold=?, units_saved=?, kg_saved=?, value_recovered=?, completed_date=? WHERE id=?",
        sold, saved, kg, val, Date.valueOf(LocalDate.now()), id);
    return Map.of("unitsSaved", saved, "kgSaved", kg, "valueRecovered", val);
  }

  // ---------------------------------------------------------------- impact + insights
  Map<String, Object> impact(long shopId) {
    Map<String, Object> r = rows(jdbc, "SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0) AS done, "
        + "COALESCE(SUM(units_saved),0) AS units, COALESCE(SUM(kg_saved),0) AS kg, COALESCE(SUM(value_recovered),0) AS val, "
        + "COUNT(DISTINCT CASE WHEN units_saved>0 THEN product_id END) AS prods FROM interventions WHERE shop_id=?", shopId).get(0);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("interventions", lng(r.get("total"))); m.put("completed", lng(r.get("done")));
    m.put("unitsSaved", lng(r.get("units"))); m.put("kgSaved", r1(num(r.get("kg"))));
    m.put("valueRecovered", Math.round(num(r.get("val")))); m.put("productsSaved", lng(r.get("prods")));
    return m;
  }

  @GetMapping("/impact")
  public Map<String, Object> impactApi(@RequestAttribute("uid") long uid, @RequestParam long shopId) {
    own(uid, shopId);
    return impact(shopId);
  }

  @GetMapping("/public/shop/{code}")
  public Map<String, Object> publicShop(@PathVariable String code) {
    List<Map<String, Object>> s = rows(jdbc, "SELECT id, code, name, city FROM shops WHERE code=?", code);
    if (s.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found");
    Map<String, Object> m = new LinkedHashMap<>(s.get(0));
    m.put("impact", impact(lng(s.get(0).get("id"))));
    m.remove("id");
    return m;
  }

  @GetMapping("/insights")
  public Map<String, Object> insights(@RequestAttribute("uid") long uid, @RequestParam long shopId) {
    own(uid, shopId);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("seasonality", fs.seasonMap());
    m.put("actual", rows(jdbc, "SELECT p.category AS category, YEAR(s.sale_date) AS y, MONTH(s.sale_date) AS m, AVG(s.units) AS avg_units, COUNT(*) AS n "
        + "FROM sales s JOIN products p ON p.id=s.product_id WHERE p.shop_id=? GROUP BY p.category, YEAR(s.sale_date), MONTH(s.sale_date) ORDER BY 1,2,3", shopId));
    m.put("month", LocalDate.now().getMonthValue());
    return m;
  }
}
