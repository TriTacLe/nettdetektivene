package ntnu.no.backend.config;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.security.JWTAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Spring Security for stateless JWT authentication.
 * Defines public endpoints, CORS policy and the filter chain.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see JWTAuthFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JWTAuthFilter jwtAuthFilter;
  private final AuthenticationProvider authenticationProvider;

  private static final String[] PUBLIC_ENDPOINTS = {
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/uploads/**",
    "/api/v1/auth/feide",
    "/api/v1/auth/login",
    "/api/v1/auth/register",
    "/api/v1/auth/refresh",
    "/api/v1/auth/logout"
  };


  /**
   * Configures the security filter chain with stateless session management,
   * public endpoint rules and the JWT auth filter.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
        .anyRequest().authenticated()
      )
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authenticationProvider(authenticationProvider)
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configures CORS to allow requests from local development origins.
   *
   * @return a {@link WebMvcConfigurer} with the CORS mappings applied
   */
  @Bean
  public WebMvcConfigurer corsConfig() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
          .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
          .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
          .allowedHeaders("*")
          .allowCredentials(true)
          .maxAge(3600);
        registry.addMapping("/uploads/**")
          .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
          .allowedMethods("GET")
          .allowedHeaders("*");
      }
    };
  }
}
