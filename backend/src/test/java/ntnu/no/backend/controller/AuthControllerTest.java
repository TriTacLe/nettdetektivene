package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RefreshRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.exception.RefreshTokenException;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.AuthService;
import ntnu.no.backend.service.FeideAuthService;
import ntnu.no.backend.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link AuthController}.
 * Verifies HTTP status codes and JSON structure for authentication endpoints.
 * Security filters are disabled because all auth endpoints are public.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

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
  void register_returnsCreated() throws Exception {
    RegisterRequestDTO dto = new RegisterRequestDTO();
    dto.setDisplayName("Test User");
    dto.setEmail("test@example.com");
    dto.setPassword("securePassword123");
    dto.setRole("sys_admin");

    AuthResponseDTO response = new AuthResponseDTO("jwt.token.here", "refresh.token.here", List.of("sys_admin"));
    when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("jwt.token.here"))
        .andExpect(jsonPath("$.refreshToken").value("refresh.token.here"))
        .andExpect(jsonPath("$.roles[0]").value("sys_admin"));
  }

  @Test
  void register_duplicateEmail_returnsConflict() throws Exception {
    RegisterRequestDTO dto = new RegisterRequestDTO();
    dto.setDisplayName("Test User");
    dto.setEmail("existing@example.com");
    dto.setPassword("securePassword123");
    dto.setRole("sys_admin");

    when(authService.register(any(RegisterRequestDTO.class)))
        .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict());
  }

  @Test
  void register_invalidInput_returnsBadRequest() throws Exception {
    RegisterRequestDTO dto = new RegisterRequestDTO();

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_returnsOk() throws Exception {
    LoginRequestDTO dto = new LoginRequestDTO();
    dto.setEmail("test@example.com");
    dto.setPassword("securePassword123");

    AuthResponseDTO response = new AuthResponseDTO("jwt.token.here", "refresh.token.here", List.of("sys_admin"));
    when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt.token.here"))
        .andExpect(jsonPath("$.refreshToken").value("refresh.token.here"));
  }

  @Test
  void login_invalidCredentials_returnsUnauthorized() throws Exception {
    LoginRequestDTO dto = new LoginRequestDTO();
    dto.setEmail("test@example.com");
    dto.setPassword("wrongPassword");

    when(authService.login(any(LoginRequestDTO.class)))
        .thenThrow(new BadCredentialsException("Invalid email or password"));

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_invalidInput_returnsBadRequest() throws Exception {
    LoginRequestDTO dto = new LoginRequestDTO();

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refresh_returnsOk() throws Exception {
    RefreshRequestDTO dto = new RefreshRequestDTO();
    dto.setRefreshToken("valid.refresh.token");
    dto.setExpiredToken("expired.jwt.token");

    AuthResponseDTO response = new AuthResponseDTO("new.jwt.token", "new.refresh.token", List.of("sys_admin"));
    when(refreshTokenService.refresh(eq("valid.refresh.token"), eq("expired.jwt.token"))).thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new.jwt.token"))
        .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));
  }

  @Test
  void refresh_invalidToken_returnsUnauthorized() throws Exception {
    RefreshRequestDTO dto = new RefreshRequestDTO();
    dto.setRefreshToken("invalid.refresh.token");

    when(refreshTokenService.refresh(eq("invalid.refresh.token"), any()))
        .thenThrow(new RefreshTokenException("Refresh token not found"));

    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_invalidInput_returnsBadRequest() throws Exception {
    RefreshRequestDTO dto = new RefreshRequestDTO();

    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logout_returnsNoContent() throws Exception {
    RefreshRequestDTO dto = new RefreshRequestDTO();
    dto.setRefreshToken("valid.refresh.token");

    doNothing().when(refreshTokenService).revoke("valid.refresh.token");

    mockMvc.perform(post("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isNoContent());
  }

  @Test
  void logout_invalidToken_returnsUnauthorized() throws Exception {
    RefreshRequestDTO dto = new RefreshRequestDTO();
    dto.setRefreshToken("invalid.refresh.token");

    doThrow(new RefreshTokenException("Refresh token not found"))
        .when(refreshTokenService).revoke("invalid.refresh.token");

    mockMvc.perform(post("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnauthorized());
  }
}