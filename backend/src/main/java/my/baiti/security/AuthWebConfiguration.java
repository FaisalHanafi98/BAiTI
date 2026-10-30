package my.baiti.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
public class AuthWebConfiguration {

  @Bean
  public OriginPolicy originPolicy(@Value("${baiti.security.allowed-origins}") String value) {
    return OriginPolicy.parse(value);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(OriginPolicy policy) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.copyOf(policy.origins()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-Tenant-Slug", "X-BAiTI-Refresh"));
    config.setAllowCredentials(true);
    config.setMaxAge(600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
