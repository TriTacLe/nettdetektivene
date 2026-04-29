package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.dto.classroom.JoinClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.ClassroomStudentService;
import ntnu.no.backend.util.ClassroomTestFactory;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link ClassroomStudentController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(ClassroomStudentController.class)
@Import(TestSecurityConfig.class)
class ClassroomStudentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ClassroomStudentService classroomStudentService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser(authorities = "student")
  void getMyClassroom_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.getMyClassroom(any()))
      .thenReturn(ClassroomTestFactory.responseDTO(classroomId));

    mockMvc.perform(get("/api/v1/classrooms/me"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.title").value("Cybersecurity 101"))
      .andExpect(jsonPath("$.data.id").value(classroomId.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getMyClassroom_asInstructor_returnsForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/me"))
      .andExpect(status().isForbidden());
  }

  @Test
  void getMyClassroom_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/me"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = "student")
  void getMyMembership_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.getMyMembership(any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    mockMvc.perform(get("/api/v1/classrooms/me/status"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "student")
  void join_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.join(any(), any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    JoinClassroomRequestDTO request = new JoinClassroomRequestDTO();
    request.setJoinCode("JOIN123");

    mockMvc.perform(post("/api/v1/classrooms/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .with(csrf()))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "student")
  void join_withInvalidJoinCode_returnsBadRequest() throws Exception {
    when(classroomStudentService.join(any(), any()))
      .thenThrow(new IllegalArgumentException("Invalid join code"));

    JoinClassroomRequestDTO request = new JoinClassroomRequestDTO();
    request.setJoinCode("INVALID");

    mockMvc.perform(post("/api/v1/classrooms/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .with(csrf()))
      .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void join_asInstructor_returnsForbidden() throws Exception {
    JoinClassroomRequestDTO request = new JoinClassroomRequestDTO();
    request.setJoinCode("JOIN123");

    mockMvc.perform(post("/api/v1/classrooms/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getStudents_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    when(classroomStudentService.getStudents(eq(classroomId), any()))
      .thenReturn(List.of(
        ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId1),
        ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId2)
      ));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/students"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data[0].classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data[0].userId").value(userId1.toString()))
      .andExpect(jsonPath("$.data[1].userId").value(userId2.toString()));
  }

  @Test
  @WithMockUser(authorities = "student")
  void getStudents_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/students"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getApprovedStudents_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();

    when(classroomStudentService.getApprovedStudents(eq(classroomId), any()))
      .thenReturn(List.of(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId1)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/students/approved"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data[0].classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data[0].userId").value(userId1.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getPendingStudents_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();

    when(classroomStudentService.getPendingStudents(eq(classroomId), any()))
      .thenReturn(List.of(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId1)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/students/pending"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data[0].classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data[0].userId").value(userId1.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getPendingNameChanges_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();

    when(classroomStudentService.getPendingNameChanges(eq(classroomId), any()))
      .thenReturn(List.of(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId1)));

    mockMvc.perform(get("/api/v1/classrooms/" + classroomId + "/students/pending-names"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data[0].classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data[0].userId").value(userId1.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void approve_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.approve(eq(classroomId), eq(userId), any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/students/" + userId + "/approve")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void approveNameChange_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.approveNameChange(eq(classroomId), eq(userId), any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/students/" + userId + "/approve-name")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void rejectNameChange_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.rejectNameChange(eq(classroomId), eq(userId), any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/students/" + userId + "/reject-name")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void resetDisplayName_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomStudentService.resetDisplayName(eq(classroomId), eq(userId), any()))
      .thenReturn(ClassroomTestFactory.classroomStudentResponseDTO(classroomId, userId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/students/" + userId + "/reset-name")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.userId").value(userId.toString()));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void remove_returnsNoContent() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    doNothing().when(classroomStudentService).remove(eq(classroomId), eq(userId), any());

    mockMvc.perform(delete("/api/v1/classrooms/" + classroomId + "/students/" + userId)
        .with(csrf()))
      .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = "student")
  void remove_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/classrooms/" + classroomId + "/students/" + userId)
        .with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void approve_missingCsrf_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/students/" + userId + "/approve"))
      .andExpect(status().isForbidden());
  }
}