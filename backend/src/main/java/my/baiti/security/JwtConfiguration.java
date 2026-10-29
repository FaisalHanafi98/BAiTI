package my.baiti.security;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

  @Bean
  public Clock authenticationClock() {
    return Clock.systemUTC();
  }

  @Bean
  public JwtService jwtService(@Value("${baiti.security.jwt.secret}") String secret, Clock clock) {
    return new JwtService(secret, clock);
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    // Password login arrives through the tenant-aware service; disable generated user fallback.
    return authentication -> {
      throw new BadCredentialsException("Authentication failed");
    };
  }
}
