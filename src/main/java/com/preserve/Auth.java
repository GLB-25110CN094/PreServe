package com.preserve;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Simple bearer-token session store + interceptor protecting /api/** (except auth and public). */
@Component
public class Auth implements HandlerInterceptor {
  private final Map<String, Long> tokens = new ConcurrentHashMap<>();

  public String issue(long uid) { String t = UUID.randomUUID().toString(); tokens.put(t, uid); return t; }
  public void revoke(String t) { tokens.remove(t); }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) throws Exception {
    if (req.getMethod().equals("OPTIONS")) return true;
    String a = req.getHeader("Authorization");
    Long uid = (a != null && a.startsWith("Bearer ")) ? tokens.get(a.substring(7)) : null;
    if (uid == null) { res.sendError(401, "Please login again"); return false; }
    req.setAttribute("uid", uid);
    return true;
  }

  @Configuration
  static class Cfg implements WebMvcConfigurer {
    private final Auth auth;
    Cfg(Auth auth) { this.auth = auth; }
    @Override
    public void addInterceptors(InterceptorRegistry r) {
      r.addInterceptor(auth).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**", "/api/public/**");
    }
  }
}
