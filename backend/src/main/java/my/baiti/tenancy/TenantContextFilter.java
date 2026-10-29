package my.baiti.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public final class TenantContextFilter extends OncePerRequestFilter {

  private final TenantResolver resolver;

  public TenantContextFilter(TenantResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    TenantContext.clear();
    try {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      boolean platform = path.equals("/api/admin") || path.startsWith("/api/admin/");
      boolean health = path.equals("/actuator/health") || path.startsWith("/actuator/health/");
      if (!platform && !health) {
        try {
          resolver.resolve(request).ifPresent(TenantContext::set);
        } catch (TenantResolutionException failure) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json");
          response.getWriter().write("{\"message\":\"Authentication failed\"}");
          return;
        }
      }
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
