package ntnu.no.backend.controller;

import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.LeaderboardService;
import ntnu.no.backend.util.LeaderboardTestFactory;
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
 * Controller tests for {@link LeaderboardController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

  /**
   * Simulate HTTP req without a server running
   */
  @Autowired
  MockMvc mockMvc;

  /**
   * Fake leaderboard service that never touches the db
   */
  @MockitoBean
  LeaderboardService leaderboardService;

  /** Required to satisfy JWTAuthFilter in the security filter chain. */
  @MockitoBean
  JWTUtil jwtUtil;

  /** Required to satisfy JWTAuthFilter in the security filter chain. */
  @MockitoBean
  UserDetailsService userDetailsService;

  /**
   * Verifies that an authenticated user gets 200 and a ranked leaderboard list.
   */
  @Test
  @WithMockUser
  void getClassroomLeaderboard_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(leaderboardService.getClassroomLeaderboard(classroomId))
        .thenReturn(List.of(LeaderboardTestFactory.entryResponseDTO(userId)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/leaderboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].displayName").value("Tri"));
  }

  /**
   * Verifies that an authenticated user gets 200 and a list of task scores for a student.
   */
  @Test
  @WithMockUser
  void getStudentScore_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    when(leaderboardService.getStudentScore(classroomId, userId))
        .thenReturn(List.of(LeaderboardTestFactory.scoreResponseDTO(userId, taskId, classroomId)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/leaderboard/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].bestScore").value(80));
  }

  /**
   * Verifies that an authenticated user gets 200 and the passed task count for a stop location.
   */
  @Test
  @WithMockUser
  void countPassedTasksByStopLocation_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String stopLocation = "STOP_1";

    when(leaderboardService.countPassedTasksByStopLocation(classroomId, userId, stopLocation))
        .thenReturn(5L);

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/leaderboard/" + userId + "/passed-count")
            .param("stopLocation", stopLocation))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(5));
  }

  /**
   * Verifies that a missing required stopLocation parameter returns 400.
   */
  @Test
  @WithMockUser
  void countPassedTasksByStopLocation_missingParam_returnsBadRequest() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/leaderboard/" + userId + "/passed-count"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Verifies that an unauthenticated request to the leaderboard returns 401.
   */
  @Test
  void getClassroomLeaderboard_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/" + UUID.randomUUID() + "/leaderboard"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Verifies that an unauthenticated request for a student's scores returns 401.
   */
  @Test
  void getStudentScore_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/" + UUID.randomUUID() + "/leaderboard/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Verifies that an unauthenticated request for passed-count returns 401.
   */
  @Test
  void countPassedTasksByStopLocation_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/" + UUID.randomUUID()
            + "/leaderboard/" + UUID.randomUUID() + "/passed-count")
            .param("stopLocation", "STOP_1"))
        .andExpect(status().isUnauthorized());
  }
}