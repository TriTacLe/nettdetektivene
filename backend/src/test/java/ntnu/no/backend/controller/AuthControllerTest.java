package ntnu.no.backend.controller;

import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.AuthService;
import ntnu.no.backend.service.FeideAuthService;
import ntnu.no.backend.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Controller tests for {@link AuthController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private FeideAuthService feideAuthService;

  @MockitoBean
  private RefreshTokenService refreshTokenService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void register_shouldReturnOk() throws Exception {
    // TODO: Implement test when register endpoint is complete
  }

  @Test
  void login_shouldReturnOk() throws Exception {
    // TODO: Implement test when login endpoint is complete
  }
}
