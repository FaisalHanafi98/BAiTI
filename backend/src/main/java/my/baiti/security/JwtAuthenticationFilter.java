package my.baiti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import my.baiti.common.ApiError;
import my.baiti.tenancy.TenantContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwt;

  public JwtAuthenticationFilter(JwtService jwt) {
    this.jwt = jwt;
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
    try {
      if (headers.size() != 1 || !headers.get(0).regionMatches(true, 0, "Bearer ", 0, 7)) {
        throw new BadCredentialsException("Authentication failed");
      }
      var principal = jwt.verify(headers.get(0).substring(7));
      String path = request.getRequestURI().substring(request.getContextPath().length());
      if (path.equals("/api/admin") || path.startsWith("/api/admin/")) {
        reject(request, response, HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      if (!TenantContext.current().filter(principal.tenantId()::equals).isPresent()) {
        throw new BadCredentialsException("Authentication failed");
      }
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              principal,
              null,
              principal.permissions().stream().map(SimpleGrantedAuthority::new).toList());
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      chain.doFilter(request, response);
    } catch (BadCredentialsException invalid) {
      reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  public static void reject(HttpServletRequest request, HttpServletResponse response, int status)
      throws IOException {
    ApiError.write(
        request,
        response,
        status,
        status == HttpServletResponse.SC_UNAUTHORIZED ? "Authentication failed" : "Access denied");
  }
}
