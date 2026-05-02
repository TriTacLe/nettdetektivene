package ntnu.no.backend.integration;

import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.RoleRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.security.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig.
 * Verifies that the security filter chain correctly enforces authentication
 * and authorization rules.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JWTUtil jwtUtil;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  private User testUser;
  private String validToken;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    roleRepository.deleteAll();

    Role instructorRole = new Role();
    instructorRole.setRoleName("instructor");
    roleRepository.save(instructorRole);

    testUser = User.builder()
        .email("test@gmail.com")
        .displayName("Test")
        .isActive(true)
        .roles(List.of(instructorRole))
        .build();
    userRepository.save(testUser);

    validToken = jwtUtil.generateToken(testUser);
  }

  /**
   * Verifies that public auth endpoints are accessible without authentication.
   */
  @Test
  void publicEndpoint_isAccessible_withoutAuth() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@gmail.com\",\"password\":\"wrong\"}"))
        .andExpect(status().is4xxClientError());
  }

  /**
   * Verifies that protected endpoints return 401 without a token.
   */
  @Test
  void protectedEndpoint_returns401_withoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Authentication required"));
  }

  /**
   * Verifies that protected endpoints accept a valid JWT token.
   */
  @Test
  void protectedEndpoint_returns200_withValidToken() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms")
            .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk());
  }

  /**
   * Verifies that an invalid token returns 401.
   */
  @Test
  void protectedEndpoint_returns401_withInvalidToken() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms")
            .header("Authorization", "Bearer invalid.token.here"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Verifies that an expired token returns 401.
   */
  @Test
  void protectedEndpoint_returns401_withExpiredToken() throws Exception {
    java.lang.reflect.Field field = JWTUtil.class.getDeclaredField("tokenValidity");
    field.setAccessible(true);
    long original = (long) field.get(jwtUtil);
    field.set(jwtUtil, -1000L);
    String expiredToken = jwtUtil.generateToken(testUser);
    field.set(jwtUtil, original);

    mockMvc.perform(get("/api/v1/classrooms")
            .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Verifies that Swagger API docs are publicly accessible.
   */
  @Test
  void swaggerEndpoint_isPublic() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());
  }
}
