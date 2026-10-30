package my.baiti.security;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenLifetimes.class)
public class JwtConfiguration {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public Clock authenticationClock() {
    return Clock.systemUTC();
  }

  @Bean
  public JwtService jwtService(
      @Value("${baiti.security.jwt.secret}") String secret, Clock clock, TokenLifetimes limits) {
    return new JwtService(secret, clock, limits.access());
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    // Password login arrives through the tenant-aware service; disable generated user fallback.
    return authentication -> {
      throw new BadCredentialsException("Authentication failed");
    };
  }
}
