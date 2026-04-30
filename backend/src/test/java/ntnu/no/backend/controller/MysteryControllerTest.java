package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.mystery.MysteryCompletionRequestDTO;
import ntnu.no.backend.dto.mystery.MysteryCompletionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryPlayDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewActionDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.MysteryService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MysteryController.class)
@Import(TestSecurityConfig.class)
class MysteryControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private MysteryService mysteryService;
  @MockitoBean private JWTUtil jwtUtil;
  @MockitoBean private UserDetailsService userDetailsService;

  @Test
  void submitMystery_asStudent_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    when(mysteryService.submitMystery(any(), any(), eq(userId)))
        .thenReturn(reviewDto(UUID.randomUUID(), classroomId, userId, MysteryStatus.PENDING));

    MockMultipartFile imagePart = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[]{1, 2, 3});
    MockMultipartFile submissionPart = new MockMultipartFile(
        "submission",
        "submission.json",
        "application/json",
        objectMapper.writeValueAsBytes(request(classroomId))
    );

    mockMvc.perform(multipart("/api/v1/mysteries")
            .file(submissionPart)
            .file(imagePart)
            .with(user(principal))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
        .andExpect(jsonPath("$.data.mysStatus").value("PENDING"));
  }

  @Test
  void submitMystery_asInstructor_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    MockMultipartFile imagePart = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[]{1, 2, 3});
    MockMultipartFile submissionPart = new MockMultipartFile(
        "submission",
        "submission.json",
        "application/json",
        objectMapper.writeValueAsBytes(request(classroomId))
    );

    mockMvc.perform(multipart("/api/v1/mysteries")
            .file(submissionPart)
            .file(imagePart)
            .with(user(testUser(userId, "instructor")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getPlayList_asStudent_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();

    when(mysteryService.getPlayList(eq(classroomId), eq(userId)))
        .thenReturn(List.of(new MysteryPlayDTO(submissionId, classroomId, "/uploads/mysteries/a.jpg",
            List.of("A", "B", "C"), false)));

    mockMvc.perform(get("/api/v1/mysteries/classroom/{id}/play", classroomId)
            .with(user(testUser(userId, "student"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(submissionId.toString()))
        .andExpect(jsonPath("$.data[0].alternatives.length()").value(3))
        .andExpect(jsonPath("$.data[0].correctIndex").doesNotExist());
  }

  @Test
  void completeMystery_asStudent_returnsOk() throws Exception {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysteryService.completeMystery(eq(submissionId), eq((byte) 1), eq(userId)))
        .thenReturn(new MysteryCompletionResponseDTO(true, true, false));

    MysteryCompletionRequestDTO body = MysteryCompletionRequestDTO.builder().selectedIndex((byte) 1).build();

    mockMvc.perform(post("/api/v1/mysteries/{id}/completion", submissionId)
            .with(user(testUser(userId, "student")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.correct").value(true))
        .andExpect(jsonPath("$.data.completed").value(true));
  }

  @Test
  void getSubmissionsByClassroom_asInstructor_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysteryService.getSubmissionsByClassroom(classroomId))
        .thenReturn(List.of(reviewDto(UUID.randomUUID(), classroomId, userId, MysteryStatus.PENDING)));

    mockMvc.perform(get("/api/v1/mysteries/classroom/{id}", classroomId)
            .with(user(testUser(UUID.randomUUID(), "instructor"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].correctIndex").exists());
  }

  @Test
  void getSubmissionsByClassroom_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/mysteries/classroom/{id}", classroomId)
            .with(user(testUser(UUID.randomUUID(), "student"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void reviewSubmission_asInstructor_returnsOk() throws Exception {
    UUID submissionId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID studentId = UUID.randomUUID();

    when(mysteryService.reviewSubmission(eq(submissionId), any(MysteryReviewActionDTO.class), eq(instructorId)))
        .thenReturn(reviewDto(submissionId, classroomId, studentId, MysteryStatus.APPROVED));

    mockMvc.perform(patch("/api/v1/mysteries/{id}/review", submissionId)
            .with(user(testUser(instructorId, "instructor")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(action(MysteryStatus.APPROVED))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mysStatus").value("APPROVED"));
  }

  @Test
  void reviewSubmission_asStudent_returnsForbidden() throws Exception {
    UUID submissionId = UUID.randomUUID();

    mockMvc.perform(patch("/api/v1/mysteries/{id}/review", submissionId)
            .with(user(testUser(UUID.randomUUID(), "student")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(action(MysteryStatus.APPROVED))))
        .andExpect(status().isForbidden());
  }

  private MysterySubmissionRequestDTO request(UUID classroomId) {
    return MysterySubmissionRequestDTO.builder()
        .classroomId(classroomId)
        .alternative1("A")
        .alternative2("B")
        .alternative3("C")
        .correctIndex((byte) 0)
        .build();
  }

  private MysteryReviewActionDTO action(MysteryStatus status) {
    return MysteryReviewActionDTO.builder().mysStatus(status).build();
  }

  private MysteryReviewDTO reviewDto(UUID id, UUID classroomId, UUID submittedBy, MysteryStatus status) {
    return new MysteryReviewDTO(
        id,
        classroomId,
        submittedBy,
        "Test Student",
        "/uploads/mysteries/x/y.jpg",
        List.of("A", "B", "C"),
        (byte) 0,
        status,
        status == MysteryStatus.PENDING ? null : UUID.randomUUID(),
        status == MysteryStatus.PENDING ? null : LocalDateTime.of(2026, 4, 27, 10, 0),
        LocalDateTime.of(2026, 4, 27, 9, 0)
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
