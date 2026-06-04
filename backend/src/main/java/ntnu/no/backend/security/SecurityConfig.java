package ntnu.no.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

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
  private final ObjectMapper objectMapper;

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
   * Returns 401 with a JSON body when a request has no valid authentication.
   * Critical for the frontend token-refresh interceptors, which
   * distinguish 401 trigger refresh, from 403 insufficient role.
   */
  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getOutputStream(),
          Map.of("success", false, "message", "Authentication required"));
    };
  }

  /**
   * Returns 403 with a JSON body when an authenticated user lacks the
   * required role or authority for the requested resource.
   */
  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(response.getOutputStream(),
          Map.of("success", false, "message", "Access denied"));
    };
  }

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
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint())
        .accessDeniedHandler(accessDeniedHandler())
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
          .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "https://nettdetektivene.orbitntnu.com", "https://nettdetektivene.tritacle.no", "http://localhost:4173")
          .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
          .allowedHeaders("*")
          .allowCredentials(true)
          .maxAge(3600);
        registry.addMapping("/uploads/**")
          .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "https://nettdetektivene.orbitntnu.com", "https://nettdetektivene.tritacle.no", "http://localhost:4173")
          .allowedMethods("GET")
          .allowedHeaders("*");
      }
    };
  }
}
