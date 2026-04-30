package ntnu.no.backend.controller;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.UserAchievementService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link UserAchievementController}.
 *
 * @author tri
 * @version 1.0
 * @since 1.0
 */
@WebMvcTest(UserAchievementController.class)
@Import(TestSecurityConfig.class)
@RequiredArgsConstructor
class UserAchievementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserAchievementService userAchievementService;

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

  private final UUID achievementId = UUID.randomUUID();
  private final UserAchievementResponseDTO achievement = new UserAchievementResponseDTO(
      userId,
      achievementId,
      "First Achievement",
      UUID.randomUUID(),
      LocalDateTime.now()
  );

  @Test
  void getMyAchievements_returns200_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(userAchievementService.getAchievementsForUser(userId)).thenReturn(List.of(achievement));

    mockMvc.perform(get("/api/v1/achievements/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].achievementId").value(achievementId.toString()));
  }

  @Test
  void getMyAchievements_returns401_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/achievements/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAchievementsForStudent_returns200_whenInstructor() throws Exception {
    User user = sampleUser("instructor");
    setAuthenticatedUser(user);
    when(userAchievementService.getAchievementsForUser(userId)).thenReturn(List.of(achievement));

    mockMvc.perform(get("/api/v1/achievements/user/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void getAchievementsForStudent_returns403_whenNotInstructor() throws Exception {
    User student = sampleUser("student");
    setAuthenticatedUser(student);
    mockMvc.perform(get("/api/v1/achievements/user/{userId}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAchievementsForStudent_returns200_whenSysAdmin() throws Exception {
    User sysAdmin = sampleUser("sys_admin");
    setAuthenticatedUser(sysAdmin);
    when(userAchievementService.getAchievementsForUser(userId)).thenReturn(List.of(achievement));

    mockMvc.perform(get("/api/v1/achievements/user/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void getAchievementsForStudent_returns401_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/achievements/user/{userId}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAchievementsForStudent_returns403_whenStudent() throws Exception {
    User student = sampleUser("student");
    setAuthenticatedUser(student);
    mockMvc.perform(get("/api/v1/achievements/user/{userId}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }
}