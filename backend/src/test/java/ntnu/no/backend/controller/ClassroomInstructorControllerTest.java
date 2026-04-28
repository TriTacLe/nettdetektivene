package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.ClassroomInstructorService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link ClassroomInstructorController}.
 *
 * @author norakirknes
 * @version 1.0
 * @since 1.0
 */
@WebMvcTest(ClassroomInstructorController.class)
@Import(TestSecurityConfig.class)
@RequiredArgsConstructor
class ClassroomInstructorControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ClassroomInstructorService classroomInstructorService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  private final UUID classroomId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final ClassroomInstructorResponseDTO instructor = 
      new ClassroomInstructorResponseDTO(classroomId, userId, LocalDateTime.now());

  @Test
  @WithMockUser(authorities = "instructor")
  void getInstructors_returnsList_whenInstructor() throws Exception {
    when(classroomInstructorService.getInstructors(eq(classroomId), any(User.class)))
        .thenReturn(List.of(instructor));

    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/instructors", classroomId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @WithMockUser(authorities = {"student"})
  void getInstructors_returns403_whenNotInstructor() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/{classroomId}/instructors", classroomId))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void getMyClassrooms_returns200_whenAuthorized() throws Exception {
    when(classroomInstructorService.getMyClassrooms(any(User.class)))
        .thenReturn(List.of(instructor));

    mockMvc.perform(get("/api/v1/classrooms/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @WithMockUser(authorities = {"student"})
  void getMyClassrooms_returns403_whenNotInstructor() throws Exception {
    mockMvc.perform(get("/api/v1/classrooms/mine"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void addInstructor_returns201_whenAuthorized() throws Exception {
    when(classroomInstructorService.addInstructor(eq(classroomId), eq(userId), any(User.class)))
        .thenReturn(instructor);

    mockMvc.perform(post("/api/v1/classrooms/{classroomId}/instructors/{userId}", classroomId, userId)
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(authorities = {"student"})
  void addInstructor_returns403_whenNotInstructor() throws Exception {
    mockMvc.perform(post("/api/v1/classrooms/{classroomId}/instructors/{userId}", classroomId, userId)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void removeInstructor_returns204_whenAuthorized() throws Exception {
    mockMvc.perform(delete("/api/v1/classrooms/{classroomId}/instructors/{userId}", classroomId, userId)
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = {"student"})
  void removeInstructor_returns403_whenNotInstructor() throws Exception {
    mockMvc.perform(delete("/api/v1/classrooms/{classroomId}/instructors/{userId}", classroomId, userId)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }
}