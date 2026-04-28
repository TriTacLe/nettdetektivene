package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.UserProfileService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link UserProfileController}.
 *
 * @author norakirknes
 * @version 1.0
 * @since 1.0
 */
@WebMvcTest(UserProfileController.class)
@Import(TestSecurityConfig.class)
@RequiredArgsConstructor
class UserProfileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserProfileService userProfileService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  private final UUID userId = UUID.randomUUID();

  private User sampleUser(String... roleNames) {
    List<Role> roles = new ArrayList<>();
    for (String roleName : roleNames) {
      Role role = new Role();
      role.setRoleName(roleName);
      roles.add(role);
    }

    return User.builder()
        .id(userId)
        .displayName("Test User")
        .email("test@example.com")
        .roles(roles)
        .build();
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
  private final UserProfileResponseDTO profile = new UserProfileResponseDTO(
      userId,
      "Test User",
      "feide123",
      "{}",
      100,
      5,
      LocalDateTime.now(),
      "NONE"
  );

  @Test
  void getProfile_returns200_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(userProfileService.getByUser(any(User.class))).thenReturn(profile);

    mockMvc.perform(get("/api/v1/profiles/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.displayName").value("Test User"));
  }

  @Test
  void getProfile_returns401_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/profiles/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateProfile_returns200_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    String json = """
      {
        "displayName": "Updated User",
        "avatarConfig": "{}"
      }
      """;

    when(userProfileService.update(any(User.class), any())).thenReturn(profile);

    mockMvc.perform(patch("/api/v1/profiles/me")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void updateProfile_returns401_whenNotAuthenticated() throws Exception {
    String json = """
      {
        "displayName": "Test",
        "avatarConfig": "{}"
      }
      """;

    mockMvc.perform(patch("/api/v1/profiles/me")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getProfileByFeideId_returns200_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(userProfileService.getByFeideId("feide123")).thenReturn(profile);

    mockMvc.perform(get("/api/v1/profiles/{feideId}", "feide123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.displayName").value("Test User"));
  }

  @Test
  void getProfileByFeideId_returns401_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/profiles/{feideId}", "feide123"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getProfileByFeideId_returns200_whenFound() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(userProfileService.getByFeideId("existing")).thenReturn(profile);

    mockMvc.perform(get("/api/v1/profiles/{feideId}", "existing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}