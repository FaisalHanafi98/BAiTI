package my.baiti.config;

import my.baiti.security.JwtAuthenticationFilter;
import my.baiti.security.JwtService;
import my.baiti.tenancy.TenantContextFilter;
import my.baiti.tenancy.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, TenantResolver resolver, JwtService jwt)
      throws Exception {
    http.addFilterBefore(
            new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new TenantContextFilter(resolver), JwtAuthenticationFilter.class)
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, failure) ->
                            JwtAuthenticationFilter.reject(response, 401))
                    .accessDeniedHandler(
                        (request, response, failure) ->
                            JwtAuthenticationFilter.reject(response, 403)));
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/actuator/health")
                .permitAll()
                .requestMatchers("/api/admin/**")
                .denyAll()
                .anyRequest()
                .authenticated());
    return http.build();
  }
}
