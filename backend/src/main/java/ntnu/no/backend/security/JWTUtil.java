package ntnu.no.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for JWT token operations.
 * Generates signed tokens on login/register, validates
 * incoming tokens on protected requests, and extracts
 * claims such as the user's email and roles.
 *
 * <p>The signing secret and token lifespan are loaded
 * from the {@code .env} file via {@code application.properties}.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
public class JWTUtil {

  /**
   * Base64-encoded HMAC-SHA256 secret key, injected from {@code jwt.secret}.
   */
  @Value("${jwt.secret}")
  private String secretKey;

  /**
   * Token lifespan in milliseconds, injected from {@code jwt.expiration-ms}.
   */
  @Value("${jwt.expiration-ms}")
  private long tokenValidity;

  /**
   * Generates a signed JWT token for the given user.
   * Embeds the user's roles as a {@code roles} claim.
   *
   * @param userDetails the authenticated user
   * @return a compact, signed JWT string
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    List<String> roles = userDetails.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.toList());
    claims.put("roles", roles);
    return buildToken(claims, userDetails);
  }

  /**
   * Generates a signed JWT for a FEIDE-authenticated user, embedding
   * {@code feideId} as a custom claim alongside the user's roles.
   * In a real FEIDE flow, the caller would pass the {@code sub} claim
   * extracted from FEIDE's verified token. In this simulation, the caller
   * passes a server-generated UUID, since the OAuth handshake cannot be
   * performed in a simulation. The token itself is identical in both cases.
   *
   * @param userDetails the authenticated user
   * @param feideId the FEIDE identifier to embed as a custom claim
   * @return a compact, signed JWT string
   */
  public String generateToken(UserDetails userDetails, String feideId) {
    Map<String, Object> claims = new HashMap<>();
    List<String> roles = userDetails.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.toList());
    claims.put("roles", roles);
    claims.put("feideId", feideId);
    return buildToken(claims, userDetails);
  }

  /**
   * Constructs and signs the JWT with subject, issue time,
   * expiration, and custom claims.
   *
   * @param claims additional data to embed in the token payload
   * @param userDetails the user the token is issued for
   * @return a signed JWT token string
   */
  private String buildToken(Map<String, Object> claims, UserDetails userDetails) {
    return Jwts.builder()
      .setClaims(claims)
      .setSubject(userDetails.getUsername())
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + tokenValidity))
      .signWith(getSigningKey(), SignatureAlgorithm.HS256)
      .compact();
  }

  /**
   * Validates a token by verifying the email matches the given user
   * and that the token has not expired.
   *
   * @param token the JWT string to validate
   * @param userDetails the user to validate against
   * @return {@code true} if valid, {@code false} otherwise
   */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    return extractEmail(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  /**
   * Returns {@code true} if the token's expiration time is in the past.
   *
   * @param token the JWT string to check
   * @return {@code true} if expired
   */
  private boolean isTokenExpired(String token) {
    return extractTokenClaim(token, Claims::getExpiration).before(new Date());
  }

  /**
   * Extracts the email (subject claim) from the token.
   *
   * @param token the JWT string
   * @return the email stored as the subject
   */
  public String extractEmail(String token) {
    return extractTokenClaim(token, Claims::getSubject);
  }

  /**
   *  Extracts the FEIDE identifier claim from the token, if present.
   *  Returns {@code null} for tokens issued to non-FEIDE users
   *  authenticated via local credentials.
   *
   * @param token the JWT string
   * @return the feideId claim value, or {@code null} if absent
   */
  public String extractFeideId(String token) {
    return extractTokenClaim(token, claims -> claims.get("feideId", String.class));
  }

  /**
   * Extracts a single claim from the token using the provided extractor function.
   *
   * @param token the JWT string
   * @param extractor function that maps {@link Claims} to the desired value
   * @param <T> the type of the extracted value
   * @return the extracted claim value
   */
  public <T> T extractTokenClaim(String token, Function<Claims, T> extractor) {
    return extractor.apply(extractAllTokenClaims(token));
  }

  /**
   * Parses and verifies the token signature, returning all claims.
   *
   * @param token the JWT string from the Authorization header
   * @return the verified {@link Claims} payload
   */
  private Claims extractAllTokenClaims(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(getSigningKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  /**
   * Decodes the Base64 secret and wraps it as an HMAC-SHA256 signing key.
   *
   * @return the {@link Key} used to sign and verify tokens
   */
  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
  }
}
