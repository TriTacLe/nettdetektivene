package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.TaskAttemptService;
import ntnu.no.backend.util.TaskAttemptTestFactory;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link TaskAttemptController}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(TaskAttemptController.class)
@Import(TestSecurityConfig.class)
class TaskAttemptControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TaskAttemptService taskAttemptService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void submitAttempt_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskAttemptService.submitAttempt(any(), any())).thenReturn(TaskAttemptTestFactory.responseDTO(id));

    mockMvc.perform(post("/api/v1/attempts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                TaskAttemptTestFactory.requestDTO(UUID.randomUUID(), UUID.randomUUID())))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.score").value(85))
        .andExpect(jsonPath("$.data.passed").value(true));
  }

  @Test
  void submitAttempt_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/attempts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                TaskAttemptTestFactory.requestDTO(UUID.randomUUID(), UUID.randomUUID())))
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void getAttemptsByUserAndTask_returnsOk() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    when(taskAttemptService.getAttemptsByUserAndTask(userId, taskId))
        .thenReturn(List.of(TaskAttemptTestFactory.responseDTO(UUID.randomUUID())));

    mockMvc.perform(get("/api/v1/attempts/user/" + userId + "/task/" + taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].score").value(85));
  }

  @Test
  @WithMockUser
  void getAttemptsByClassroom_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    when(taskAttemptService.getAttemptsByClassroom(classroomId))
        .thenReturn(List.of(TaskAttemptTestFactory.responseDTO(UUID.randomUUID())));

    mockMvc.perform(get("/api/v1/attempts/classroom/" + classroomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].passed").value(true));
  }

  @Test
  void getAttemptsByUserAndTask_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/attempts/user/" + UUID.randomUUID() + "/task/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }
}
