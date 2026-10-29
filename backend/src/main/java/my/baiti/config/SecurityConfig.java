package my.baiti.config;

import my.baiti.tenancy.TenantContextFilter;
import my.baiti.tenancy.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * P02 security baseline: only {@code /actuator/health} is reachable anonymously; everything else
 * requires authentication (default form login). This exists solely so the skeleton's health check
 * is observable before auth exists.
 *
 * <p>P05 replaces this entirely with the JWT filter chain (stateless, RBAC via
 * {@code @PreAuthorize}, 401 entry point, CSRF handled per the cookie refresh design).
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, TenantResolver resolver)
      throws Exception {
    http.addFilterBefore(
        new TenantContextFilter(resolver), UsernamePasswordAuthenticationFilter.class);
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/actuator/health")
                .permitAll()
                .anyRequest()
                .authenticated() // P05 locks this down with real JWT authz
        );
    return http.build();
  }
}
