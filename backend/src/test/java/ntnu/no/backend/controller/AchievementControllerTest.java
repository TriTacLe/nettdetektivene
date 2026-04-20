package ntnu.no.backend.controller;

import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.AchievementService;
import ntnu.no.backend.util.AchievementTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link AchievementController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(AchievementController.class)
class AchievementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AchievementService achievementService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void getAllAchievements_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(achievementService.getAllAchievement()).thenReturn(List.of(AchievementTestFactory.responseDTO(id)));

    mockMvc.perform(get("/api/v1/achievements"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data[0].title").value("First Mystery"));
  }

  @Test
  @WithMockUser
  void getAchievement_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(achievementService.getAchievement(id)).thenReturn(AchievementTestFactory.responseDTO(id));

    mockMvc.perform(get("/api/v1/achievements/" + id))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.title").value("First Mystery"));
  }

  @Test
  void getAllAchievements_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/achievements"))
      .andExpect(status().isUnauthorized());
  }
}
