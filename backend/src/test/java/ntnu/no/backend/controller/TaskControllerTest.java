package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.TaskService;
import ntnu.no.backend.util.TaskTestFactory;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link TaskController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(TaskController.class)
@Import(TestSecurityConfig.class)
class TaskControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TaskService taskService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void getAllTasks_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskService.getAllTask()).thenReturn(List.of(TaskTestFactory.responseDTO(id)));

    mockMvc.perform(get("/api/v1/tasks"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data[0].title").value("Spot the phishing email"));
  }

  @Test
  @WithMockUser
  void getTask_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskService.getTask(id)).thenReturn(TaskTestFactory.responseDTO(id));

    mockMvc.perform(get("/api/v1/tasks/" + id))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.title").value("Spot the phishing email"));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void createTask_returnsCreated() throws Exception {
    when(taskService.createTask(any())).thenReturn(TaskTestFactory.responseDTO(UUID.randomUUID()));

    mockMvc.perform(post("/api/v1/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(TaskTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.data.title").value("Spot the phishing email"));
  }

  @Test
  @WithMockUser(authorities = "student")
  void createTask_asStudent_returnsForbidden() throws Exception {
    mockMvc.perform(post("/api/v1/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(TaskTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void deleteTask_returnsNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(taskService).deleteTask(id);

    mockMvc.perform(delete("/api/v1/tasks/" + id).with(csrf()))
      .andExpect(status().isNoContent());
  }

  @Test
  void getAllTasks_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/tasks"))
      .andExpect(status().isUnauthorized());
  }
}
