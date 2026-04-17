package ntnu.no.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every incoming HTTP request once and validates
 * the JWT from the Authorization header. If the token is valid, the user
 * is authenticated for the duration of that request.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {
  private final JWTUtil jwtUtil;
  private final UserDetailsService userDetailsService;
  private static final Logger logger = LoggerFactory.getLogger(JWTAuthFilter.class);

  /**
   * Extracts and validates the JWT on each request.
   * Delegates to {@link #authenticateUser} if a token is present.
   *
   * @param request the incoming HTTP request
   * @param response the HTTP response
   * @param filterChain the remaining filter chain
   * @throws ServletException if a servlet error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain)
    throws ServletException, IOException {
    final String jwt = extractJwtFromRequest(request);

    if (jwt != null) {
      authenticateUser(jwt, request);
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Extracts the JWT string from the Authorization header.
   * Expects the format {@code Bearer <token>}.
   *
   * @param req the HTTP request
   * @return the raw token string, or {@code null} if not present
   */
  private String extractJwtFromRequest(HttpServletRequest req) {
    final String authHeader = req.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  /**
   * Validates the token and sets the authenticated user in the Security Config
   * if the token is valid and the request is not already authenticated.
   * If a {@code feideId} claim is present, it is extracted and stored as a
   * request attribute, making it available to the FEIDE login endpoint for
   * subsequent login resolution via the identity provider lookup.
   *
   * @param jwt the token to validate
   * @param req the HTTP request for building authentication details
   */
  private void authenticateUser(String jwt, HttpServletRequest req) {
    try {
      final String email = jwtUtil.extractEmail(jwt);
      if (email != null && isNotAuthenticated()) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (jwtUtil.isTokenValid(jwt, userDetails)) {
          setAuthentication(userDetails, req);
          String feideId = jwtUtil.extractFeideId(jwt);
          if (feideId != null) {
            req.setAttribute("feideId", feideId);
          }
        }
      }
    } catch (JwtException e) {
      logger.warn("Invalid JWT token: {}", e.getMessage());
    }
  }

  /**
   * Returns {@code true} if the current request has no existing authentication.
   *
   * @return {@code true} if unauthenticated
   */
  private boolean isNotAuthenticated() {
    return SecurityContextHolder.getContext().getAuthentication() == null;
  }

  /**
   * Builds a authentication token and sets it
   * in the SecurityContext.
   *
   * @param userDetails the authenticated user
   * @param request the HTTP request for additional details
   */
  private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
    UsernamePasswordAuthenticationToken authToken =
      new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
      );
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
    logger.debug("Authenticated user: {}", userDetails.getUsername());
  }
}
