package ntnu.no.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JWTUtil}.
 * Verifies token generation, introspection, and claim extraction.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
class JWTUtilTest {

  private JWTUtil jwtUtil;

  private static final String SECRET = "dGVzdHNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3JobWFj";
  private static final long TOKEN_VALIDITY_MS = 3600000;

  @BeforeEach
  void setUp() throws Exception {
    jwtUtil = new JWTUtil();
    setField(jwtUtil, "secretKey", SECRET);
    setField(jwtUtil, "tokenValidity", TOKEN_VALIDITY_MS);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private UserDetails createUserDetails(String email, String... roles) {
    List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    return new User(email, "", authorities);
  }

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
  }

  /**
   * Verifies that generateToken returns a non-null token with correct subject.
   */
  @Test
  void generateToken_returnsTokenWithCorrectSubject() {
    UserDetails user = createUserDetails("test@gmail.com", "ROLE_instructor");

    String token = jwtUtil.generateToken(user);

    assertThat(token).isNotNull().isNotEmpty();
    Optional<Claims> claims = jwtUtil.introspect(token);
    assertThat(claims).isPresent();
    assertThat(claims.get().getSubject()).isEqualTo("test@gmail.com");
  }

  /**
   * Verifies that generated token contains roles claim.
   */
  @Test
  void generateToken_includesRolesClaim() {
    UserDetails user = createUserDetails("nora@gmail.com", "ROLE_instructor");

    String token = jwtUtil.generateToken(user);

    Optional<Claims> claims = jwtUtil.introspect(token);
    assertThat(claims).isPresent();
    @SuppressWarnings("unchecked")
    List<String> roles = claims.get().get("roles", List.class);
    assertThat(roles).containsExactly("ROLE_instructor");
  }

  /**
   * Verifies that generateToken with feideId embeds the feideId claim.
   */
  @Test
  void generateToken_withFeideId_includesFeideIdClaim() {
    UserDetails user = createUserDetails("test@gmail.com", "ROLE_student");

    String feideId = UUID.randomUUID().toString();
    String token = jwtUtil.generateToken(user, feideId);

    Optional<Claims> claims = jwtUtil.introspect(token);
    assertThat(claims).isPresent();
    assertThat(claims.get().get("feideId", String.class)).isEqualTo(feideId);
  }

  /**
   * Verifies that generated token has a future expiration date.
   */
  @Test
  void generateToken_hasValidExpiration() {
    UserDetails user = createUserDetails("test@gmail.com", "ROLE_instructor");

    String token = jwtUtil.generateToken(user);

    Optional<Claims> claims = jwtUtil.introspect(token);
    assertThat(claims).isPresent();
    assertThat(claims.get().getExpiration()).isAfter(new Date());
  }

  /**
   * Verifies that introspect returns empty for a null token.
   */
  @Test
  void introspect_returnsEmpty_whenTokenIsNull() {
    Optional<Claims> result = jwtUtil.introspect(null);

    assertThat(result).isEmpty();
  }

  /**
   * Verifies that introspect returns empty for a malformed token.
   */
  @Test
  void introspect_returnsEmpty_whenTokenIsMalformed() {
    Optional<Claims> result = jwtUtil.introspect("not.a.valid.token");

    assertThat(result).isEmpty();
  }

  /**
   * Verifies that introspect returns empty for a token signed with a different key.
   */
  @Test
  void introspect_returnsEmpty_whenTokenSignedWithDifferentKey() {
    Key wrongKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
        "YW5vdGhlcnNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3Jo"));

    String token = Jwts.builder()
        .setSubject("test@ntnu.no")
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000))
        .signWith(wrongKey, SignatureAlgorithm.HS256)
        .compact();

    Optional<Claims> result = jwtUtil.introspect(token);

    assertThat(result).isEmpty();
  }

  /**
   * Verifies that introspect still returns claims for an expired token.
   */
  @Test
  void introspect_returnsClaims_whenTokenIsExpired() {
    String token = Jwts.builder()
        .setSubject("test@gmail.com")
        .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
        .setExpiration(new Date(System.currentTimeMillis() - 3600000))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();

    Optional<Claims> result = jwtUtil.introspect(token);

    assertThat(result).isPresent();
    assertThat(result.get().getSubject()).isEqualTo("test@gmail.com");
  }
}
