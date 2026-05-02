package ntnu.no.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JWTAuthFilter}.
 * Verifies that the filter authenticates valid tokens, rejects invalid ones,
 * and always continues the filter chain.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class JWTAuthFilterTest {

  @Mock
  private JWTUtil jwtUtil;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private JWTAuthFilter jwtAuthFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private Claims createClaims(String subject, Date expiration) {
    Map<String, Object> map = new HashMap<>();
    map.put("sub", subject);
    map.put("exp", expiration);
    DefaultClaims claims = new DefaultClaims(map);
    return claims;
  }

  private UserDetails createUserDetails(String email, String... roles) {
    List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    return new User(email, "", true, true, true, true, authorities);
  }

  /**
   * Verifies that the filter chain continues when no Authorization header is present.
   */
  @Test
  void doFilter_continuesChain_whenNoAuthHeader() throws ServletException, IOException {
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /**
   * Verifies that the filter chain continues when Authorization header is not Bearer.
   */
  @Test
  void doFilter_continuesChain_whenAuthHeaderIsNotBearer() throws ServletException, IOException {
    request.addHeader("Authorization", "abc123");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtUtil, never()).introspect(anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /**
   * Verifies that valid token sets authentication in SecurityContext.
   */
  @Test
  void doFilter_setsAuthentication_whenTokenIsValid() throws ServletException, IOException {
    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Bearer " + token);

    Claims claims = createClaims("test@gmail.com", new Date(System.currentTimeMillis() + 3600000));
    UserDetails userDetails = createUserDetails("test@gmail.com", "ROLE_instructor");

    when(jwtUtil.introspect(token)).thenReturn(Optional.of(claims));
    when(userDetailsService.loadUserByUsername("test@gmail.com")).thenReturn(userDetails);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test@gmail.com");
  }

  /**
   * Verifies that an invalid token does not set authentication.
   */
  @Test
  void doFilter_doesNotAuthenticate_whenTokenIsInvalid() throws ServletException, IOException {
    request.addHeader("Authorization", "Bearer invalid.token");

    when(jwtUtil.introspect("invalid.token")).thenReturn(Optional.empty());

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /**
   * Verifies that an expired token does not set authentication.
   */
  @Test
  void doFilter_doesNotAuthenticate_whenTokenIsExpired() throws ServletException, IOException {
    String token = "expired.jwt.token";
    request.addHeader("Authorization", "Bearer " + token);

    Claims claims = createClaims("test@gmail.com", new Date(System.currentTimeMillis() - 3600000));

    when(jwtUtil.introspect(token)).thenReturn(Optional.of(claims));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(userDetailsService, never()).loadUserByUsername(anyString());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /**
   * Verifies that authentication is not set when user is not found in the database.
   */
  @Test
  void doFilter_doesNotAuthenticate_whenUserNotFound() throws ServletException, IOException {
    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Bearer " + token);

    Claims claims = createClaims("unknown@test.no", new Date(System.currentTimeMillis() + 3600000));

    when(jwtUtil.introspect(token)).thenReturn(Optional.of(claims));
    when(userDetailsService.loadUserByUsername("unknown@test.no"))
        .thenThrow(new UsernameNotFoundException("Not found"));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  /**
   * Verifies that feideId claim is stored as a request attribute when present.
   */
  @Test
  void doFilter_setsFeideIdAttribute_whenClaimPresent() throws ServletException, IOException {
    String token = "feide.jwt.token";
    request.addHeader("Authorization", "Bearer " + token);

    Map<String, Object> map = new HashMap<>();
    map.put("sub", "test@gmail.com");
    map.put("exp", new Date(System.currentTimeMillis() + 3600000));
    String feideId = java.util.UUID.randomUUID().toString();
    map.put("feideId", feideId);
    Claims claims = new DefaultClaims(map);

    UserDetails userDetails = createUserDetails("test@gmail.com", "ROLE_student");

    when(jwtUtil.introspect(token)).thenReturn(Optional.of(claims));
    when(userDetailsService.loadUserByUsername("test@gmail.com")).thenReturn(userDetails);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertThat(request.getAttribute("feideId")).isEqualTo(feideId);
  }

  /**
   * Verifies that a disabled user is not authenticated even with a valid token.
   */
  @Test
  void doFilter_doesNotAuthenticate_whenUserIsDisabled() throws ServletException, IOException {
    String token = "valid.jwt.token";
    request.addHeader("Authorization", "Bearer " + token);

    Claims claims = createClaims("test@gmail.com", new Date(System.currentTimeMillis() + 3600000));
    UserDetails disabledUser = new User("test@gmail.com", "", false, true, true, true,
        List.of(new SimpleGrantedAuthority("ROLE_instructor")));

    when(jwtUtil.introspect(token)).thenReturn(Optional.of(claims));
    when(userDetailsService.loadUserByUsername("test@gmail.com")).thenReturn(disabledUser);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
