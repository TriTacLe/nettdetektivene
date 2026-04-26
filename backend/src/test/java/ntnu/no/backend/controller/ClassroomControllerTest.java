package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.ClassroomService;
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
 * Controller tests for {@link ClassroomController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(ClassroomController.class)
@Import(TestSecurityConfig.class)
class ClassroomControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ClassroomService classroomService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser(authorities = "instructor")
  void getAllClassrooms_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(classroomService.getClassroomsForInstructor(any())).thenReturn(List.of(ClassroomTestFactory.responseDTO(id)));

    mockMvc.perform(get("/api/v1/classrooms"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data[0].title").value("Cybersecurity 101"));
  }

  @Test
  @WithMockUser
  void getClassroom_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(classroomService.getClassroom(id)).thenReturn(ClassroomTestFactory.responseDTO(id));

    mockMvc.perform(get("/api/v1/classrooms/" + id))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.title").value("Cybersecurity 101"));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void createClassroom_returnsCreated() throws Exception {
    when(classroomService.createClassroom(any(), any())).thenReturn(ClassroomTestFactory.responseDTO(UUID.randomUUID()));

    mockMvc.perform(post("/api/v1/classrooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(ClassroomTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.data.title").value("Cybersecurity 101"));
  }

  @Test
  @WithMockUser(authorities = "student")
  void createClassroom_asStudent_returnsForbidden() throws Exception {
    mockMvc.perform(post("/api/v1/classrooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(ClassroomTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void updateClassroom_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(classroomService.updateClassroom(any(), any())).thenReturn(ClassroomTestFactory.responseDTO(id));

    mockMvc.perform(patch("/api/v1/classrooms/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(ClassroomTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.title").value("Cybersecurity 101"));
  }

  @Test
  @WithMockUser(authorities = "student")
  void updateClassroom_asStudent_returnsForbidden() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(patch("/api/v1/classrooms/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(ClassroomTestFactory.requestDTO()))
        .with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void createClassroomTask_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    when(classroomService.createClassroomTask(any(), any()))
      .thenReturn(ClassroomTestFactory.classroomTaskResponseDTO(classroomId, taskId));

    mockMvc.perform(post("/api/v1/classrooms/" + classroomId + "/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(ClassroomTestFactory.classroomTaskCreateRequestDTO(taskId)))
        .with(csrf()))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
      .andExpect(jsonPath("$.data.taskId").value(taskId.toString()))
      .andExpect(jsonPath("$.data.task.title").value("Spot the phishing email"));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void deleteClassroom_returnsNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(classroomService).deleteClassroom(id);

    mockMvc.perform(delete("/api/v1/classrooms/" + id).with(csrf()))
      .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = "student")
  void deleteClassroom_asStudent_returnsForbidden() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/classrooms/" + id).with(csrf()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void getAllClassroomsByInstitution_returnsOk() throws Exception {
    UUID institutionId = UUID.randomUUID();
    when(classroomService.getAllClassroomsByInstitution(institutionId))
      .thenReturn(List.of(ClassroomTestFactory.responseDTO(UUID.randomUUID())));

    mockMvc.perform(get("/api/v1/classrooms/institution/" + institutionId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data[0].title").value("Cybersecurity 101"));
  }

  @Test
  void getAllClassrooms_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms"))
      .andExpect(status().isUnauthorized());
  }
}
