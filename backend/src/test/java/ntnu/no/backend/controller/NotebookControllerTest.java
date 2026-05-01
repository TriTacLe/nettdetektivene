package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.NotebookService;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
 * @author tri, aninesst
 * @version 0.2
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

  @Test
  @WithMockUser(authorities = "instructor")
  void getAllNotebooks_asInstructor_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(notebookService.getNotebooksByClassroom(classroomId))
        .thenReturn(List.of(notebookResponseDTO(UUID.randomUUID(), userId, classroomId)));

    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/notebooks", classroomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].stopLocation").value("stop_3"));
  }

  @Test
  @WithMockUser(authorities = "student")
  void getAllNotebooks_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/notebooks", classroomId))
        .andExpect(status().isForbidden());
  }

  @Test
  void getMyNotebooks_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    when(notebookService.getNotebookByClassroomAndUser(eq(classroomId), eq(userId)))
        .thenReturn(List.of(notebookResponseDTO(UUID.randomUUID(), userId, classroomId)));

    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/notebooks/me", classroomId)
            .with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].stopLocation").value("stop_3"));
  }

  @Test
  void createNotebook_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    when(notebookService.createNotebook(eq(classroomId), any(), eq(userId)))
        .thenReturn(notebookResponseDTO(UUID.randomUUID(), userId, classroomId));

    mockMvc.perform(post("/api/v1/classrooms/{classroomId}/notebooks", classroomId)
            .with(user(principal))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(notebookRequestDTO())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stopLocation").value("stop_3"));
  }

  @Test
  void updateNotebook_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    when(notebookService.updateNotebook(any(), eq(notebookId), eq(userId)))
        .thenReturn(notebookResponseDTO(notebookId, userId, classroomId));

    mockMvc.perform(patch("/api/v1/classrooms/{classroomId}/notebooks/{notebookId}", classroomId, notebookId)
            .with(user(principal))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(notebookRequestDTO()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stopLocation").value("stop_3"));
  }

  @Test
  void deleteNotebook_returnsNoContent() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    doNothing().when(notebookService).deleteNotebook(eq(notebookId), eq(userId));

    mockMvc.perform(delete("/api/v1/classrooms/{classroomId}/notebooks/{notebookId}", classroomId, notebookId)
            .with(user(principal))
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void createNotebook_returnsBadRequest_whenReflectionExceedsMaxLength() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .studentReflection("a".repeat(10_001))
        .build();

    mockMvc.perform(post("/api/v1/classrooms/{classroomId}/notebooks", classroomId)
            .with(user(principal))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void updateNotebook_returnsBadRequest_whenReflectionExceedsMaxLength() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .studentReflection("a".repeat(10_001))
        .build();

    mockMvc.perform(patch("/api/v1/classrooms/{classroomId}/notebooks/{notebookId}", classroomId, notebookId)
            .with(user(principal))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void getAllNotebooks_unauthenticated_returnsUnauthorized() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/notebooks", classroomId))
        .andExpect(status().isUnauthorized());
  }

  private NotebookEntryRequestDTO notebookRequestDTO() {
    return NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .autoNotes("{\"summary\":\"auto generated\"}")
        .studentReflection("This is my reflection")
        .build();
  }

  private NotebookEntryResponseDTO notebookResponseDTO(UUID id, UUID userId, UUID classroomId) {
    return new NotebookEntryResponseDTO(
        id,
        userId,
        classroomId,
        "stop_3",
        "{\"summary\":\"auto generated\"}",
        "This is my reflection",
        LocalDateTime.of(2026, 4, 22, 12, 0),
        LocalDateTime.of(2026, 4, 22, 12, 30)
    );
  }

  private User testUser(UUID id, String authority) {
    Role role = new Role();
    role.setRoleName(authority);

    User user = new User();
    user.setEmail("test@example.com");
    user.setDisplayName("Test User");
    user.setRoles(List.of(role));
    setIdReflectively(user, id);
    return user;
  }

  private void setIdReflectively(Object target, UUID id) {
    Class<?> type = target.getClass();

    while (type != null) {
      try {
        Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
        return;
      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    throw new IllegalArgumentException("Could not find id field on " + target.getClass());
  }
}