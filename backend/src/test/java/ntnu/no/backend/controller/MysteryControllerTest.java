package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionResponseDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link MysteryController}.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(MysteryController.class)
@Import(TestSecurityConfig.class)
class MysteryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private MysteryService mysteryService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void submitMystery_asStudent_returnsCreated() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    User principal = testUser(userId, "student");

    when(mysteryService.submitMystery(any(MysterySubmissionRequestDTO.class), eq(userId)))
        .thenReturn(response(UUID.randomUUID(), classroomId, userId, MysteryStatus.PENDING, false));

    mockMvc.perform(post("/api/v1/mysteries")
            .with(user(principal))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request(classroomId))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.classroomId").value(classroomId.toString()))
        .andExpect(jsonPath("$.data.mysStatus").value("PENDING"));
  }

  @Test
  void submitMystery_asInstructor_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    mockMvc.perform(post("/api/v1/mysteries")
            .with(user(testUser(userId, "instructor")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request(classroomId))))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitMystery_withInvalidBody_returnsBadRequest() throws Exception {
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    mockMvc.perform(post("/api/v1/mysteries")
            .with(user(testUser(userId, "student")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(MysterySubmissionRequestDTO.builder().build())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getSubmissionsByClassroom_asInstructor_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysteryService.getSubmissionsByClassroom(classroomId))
        .thenReturn(List.of(response(UUID.randomUUID(), classroomId, userId, MysteryStatus.PENDING, false)));

    mockMvc.perform(get("/api/v1/mysteries/classroom/{classroomId}", classroomId)
            .with(user(testUser(UUID.randomUUID(), "instructor"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].mysDesc").value("Suspicious message"));
  }

  @Test
  void getSubmissionsByClassroom_asStudent_returnsForbidden() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/mysteries/classroom/{classroomId}", classroomId)
            .with(user(testUser(UUID.randomUUID(), "student"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getPendingSubmissions_asInstructor_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysteryService.getPendingSubmissions(classroomId))
        .thenReturn(List.of(response(UUID.randomUUID(), classroomId, userId, MysteryStatus.PENDING, false)));

    mockMvc.perform(get("/api/v1/mysteries/classroom/{classroomId}/pending", classroomId)
            .with(user(testUser(UUID.randomUUID(), "instructor"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].mysStatus").value("PENDING"));
  }

  @Test
  void reviewSubmission_asInstructor_returnsOk() throws Exception {
    UUID submissionId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID studentId = UUID.randomUUID();

    when(mysteryService.reviewSubmission(eq(submissionId), any(MysteryReviewDTO.class), eq(userId)))
        .thenReturn(response(submissionId, classroomId, studentId, MysteryStatus.APPROVED, true));

    mockMvc.perform(patch("/api/v1/mysteries/{submissionId}/review", submissionId)
            .with(user(testUser(userId, "instructor")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(review(MysteryStatus.APPROVED, true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.mysStatus").value("APPROVED"))
        .andExpect(jsonPath("$.data.isFeatured").value(true));
  }

  @Test
  void reviewSubmission_asStudent_returnsForbidden() throws Exception {
    UUID submissionId = UUID.randomUUID();

    mockMvc.perform(patch("/api/v1/mysteries/{submissionId}/review", submissionId)
            .with(user(testUser(UUID.randomUUID(), "student")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(review(MysteryStatus.APPROVED, true))))
        .andExpect(status().isForbidden());
  }

  @Test
  void reviewSubmission_withInvalidBody_returnsBadRequest() throws Exception {
    UUID submissionId = UUID.randomUUID();

    mockMvc.perform(patch("/api/v1/mysteries/{submissionId}/review", submissionId)
            .with(user(testUser(UUID.randomUUID(), "instructor")))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(MysteryReviewDTO.builder().build())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getFeaturedSubmission_asStudent_returnsOk() throws Exception {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysteryService.getFeaturedSubmission(classroomId))
        .thenReturn(response(UUID.randomUUID(), classroomId, userId, MysteryStatus.APPROVED, true));

    mockMvc.perform(get("/api/v1/mysteries/classroom/{classroomId}/featured", classroomId)
            .with(user(testUser(UUID.randomUUID(), "student"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.isFeatured").value(true));
  }

  @Test
  void getFeaturedSubmission_unauthenticated_returnsUnauthorized() throws Exception {
    UUID classroomId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/mysteries/classroom/{classroomId}/featured", classroomId))
        .andExpect(status().isUnauthorized());
  }

  private MysterySubmissionRequestDTO request(UUID classroomId) {
    return MysterySubmissionRequestDTO.builder()
        .classroomId(classroomId)
        .mysDesc("Suspicious message")
        .attachmentUrl("https://example.com/file.png")
        .build();
  }

  private MysteryReviewDTO review(MysteryStatus status, boolean featured) {
    return MysteryReviewDTO.builder()
        .mysStatus(status)
        .featured(featured)
        .build();
  }

  private MysterySubmissionResponseDTO response(
      UUID submissionId,
      UUID classroomId,
      UUID submittedBy,
      MysteryStatus status,
      boolean featured
  ) {
    return new MysterySubmissionResponseDTO(
        submissionId,
        classroomId,
        submittedBy,
        "Suspicious message",
        "https://example.com/file.png",
        status,
        status == MysteryStatus.PENDING ? null : UUID.randomUUID(),
        status == MysteryStatus.PENDING ? null : LocalDateTime.of(2026, 4, 27, 10, 0),
        featured,
        17,
        2026,
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
