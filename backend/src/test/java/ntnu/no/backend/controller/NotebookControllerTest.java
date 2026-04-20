package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.NotebookService;
import ntnu.no.backend.util.NotebookTestFactory;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link NotebookController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(NotebookController.class)
@Import(TestSecurityConfig.class)
class NotebookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private NotebookService notebookService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  /**
   * Verifies that an instructor gets 200 and a list of all notebook entries.
   */
  @Test
  @WithMockUser(roles = "instructor")
  void getAllNotebooks_asInstructor_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notebookService.getNotebooksByClassroom(classroomId))
        .thenReturn(List.of(NotebookTestFactory.responseDTO(UUID.randomUUID(), userId, classroomId)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/notebooks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].stopLocation").value("stop_3"));
  }

  /**
   * Verifies that a student gets 403 when trying to access all notebook entries.
   */
  @Test
  @WithMockUser(roles = "student")
  void getAllNotebooks_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/notebooks"))
        .andExpect(status().isForbidden());
  }

  /**
   * Verifies that an authenticated student gets 200 and their own notebook entries.
   */
  @Test
  @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
  void getMyNotebooks_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(notebookService.getNotebookByClassroomAndUser(eq(classroomId), eq(userId)))
        .thenReturn(List.of(NotebookTestFactory.responseDTO(UUID.randomUUID(), userId, classroomId)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/notebooks/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].stopLocation").value("stop_3"));
  }

  /**
   * Verifies that an authenticated student gets 201 when creating a notebook entry.
   */
  @Test
  @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
  void createNotebook_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(notebookService.createNotebook(eq(classroomId), any(), eq(userId)))
        .thenReturn(NotebookTestFactory.responseDTO(UUID.randomUUID(), userId, classroomId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/notebooks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(NotebookTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.stopLocation").value("stop_3"));
  }

  /**
   * Verifies that an authenticated user gets 200 when updating a notebook entry.
   */
  @Test
  @WithMockUser
  void updateNotebook_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notebookService.updateNotebook(any(), eq(notebookId)))
        .thenReturn(NotebookTestFactory.responseDTO(notebookId, userId, classroomId));

    mockMvc.perform(patch("/api/v1/classrooms/" + classroomId + "/notebooks/" + notebookId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(NotebookTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stopLocation").value("stop_3"));
  }

  /**
   * Verifies that an authenticated user gets 204 when deleting a notebook entry.
   */
  @Test
  @WithMockUser
  void deleteNotebook_returnsNoContent() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    doNothing().when(notebookService).deleteNotebook(notebookId);

    mockMvc.perform(delete("/api/v1/classrooms/" + classroomId + "/notebooks/" + notebookId)
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  /**
   * Verifies that an unauthenticated request returns 401.
   */
  @Test
  void getAllNotebooks_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/" + UUID.randomUUID() + "/notebooks"))
        .andExpect(status().isUnauthorized());
  }
}
