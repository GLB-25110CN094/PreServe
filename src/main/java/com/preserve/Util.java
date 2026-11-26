package com.preserve;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

public final class Util {
  private Util() {}

  /** Runs a query and returns rows with lower-case keys and dates as ISO strings (same JSON on MySQL and H2). */
  public static List<Map<String, Object>> rows(JdbcTemplate j, String sql, Object... a) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> r : j.queryForList(sql, a)) {
      Map<String, Object> m = new LinkedHashMap<>();
      for (Map.Entry<String, Object> e : r.entrySet()) {
        Object v = e.getValue();
        if (v instanceof java.sql.Date d) v = d.toString();
        else if (v instanceof java.sql.Timestamp t) v = t.toString();
        m.put(e.getKey().toLowerCase(), v);
      }
      out.add(m);
    }
    return out;
  }

  public static long insert(JdbcTemplate j, String sql, Object... a) {
    KeyHolder kh = new GeneratedKeyHolder();
    j.update(c -> {
      PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
      return ps;
    }, kh);
    return kh.getKey().longValue();
  }

  public static LocalDate ld(Object o) {
    if (o instanceof java.sql.Date d) return d.toLocalDate();
    if (o instanceof LocalDate l) return l;
    return LocalDate.parse(o.toString().substring(0, 10));
  }

  public static double num(Object o) {
    if (o == null) return 0;
    if (o instanceof Number n) return n.doubleValue();
    String s = o.toString().trim();
    return s.isEmpty() ? 0 : Double.parseDouble(s);
  }

  public static long lng(Object o) { return (long) num(o); }
  public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
  public static double r1(double v) { return Math.round(v * 10.0) / 10.0; }
}
