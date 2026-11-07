package my.baiti.tenancy.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import my.baiti.security.JwtAuthenticationFilter;
import my.baiti.security.JwtService;
import my.baiti.security.PlatformPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class PlatformTokenFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final PlatformAuthentication accounts;

  public PlatformTokenFilter(JwtService jwt, PlatformAuthentication accounts) {
    this.jwt = jwt;
    this.accounts = accounts;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var headers = Collections.list(request.getHeaders("Authorization"));
    if (headers.isEmpty()) {
      chain.doFilter(request, response);
      return;
    }
    if (headers.size() != 1 || !headers.get(0).regionMatches(true, 0, "Bearer ", 0, 7)) {
      JwtAuthenticationFilter.reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    var token = headers.get(0).substring(7);
    PlatformPrincipal principal;
    try {
      principal = jwt.verifyPlatform(token);
    } catch (BadCredentialsException invalid) {
      JwtAuthenticationFilter.reject(request, response, tenantTokenStatus(token));
      return;
    }
    if (!accounts.active(principal.adminId())) {
      JwtAuthenticationFilter.reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority(PlatformPrincipal.AUTHORITY))));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private int tenantTokenStatus(String token) {
    try {
      jwt.verify(token);
      return HttpServletResponse.SC_FORBIDDEN;
    } catch (BadCredentialsException invalid) {
      return HttpServletResponse.SC_UNAUTHORIZED;
    }
  }
}
