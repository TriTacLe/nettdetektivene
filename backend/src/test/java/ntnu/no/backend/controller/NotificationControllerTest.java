package ntnu.no.backend.controller;

import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.NotificationService;
import ntnu.no.backend.util.NotificationTestFactory;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link NotificationController}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void getMyNotifications_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    UUID recipientId = UUID.randomUUID();
    when(notificationService.getNotificationsForUser(any())).thenReturn(
        List.of(NotificationTestFactory.responseDTO(id, recipientId)));

    mockMvc.perform(get("/api/v1/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].msg").value("Din eleveksempel er godkjent"));
  }

  @Test
  void getMyNotifications_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/notifications"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void markAsRead_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    UUID recipientId = UUID.randomUUID();
    when(notificationService.markAsRead(any(), any())).thenReturn(
        NotificationTestFactory.responseDTO(id, recipientId));

    mockMvc.perform(patch("/api/v1/notifications/" + id + "/read").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void markAsRead_unauthenticated_returnsUnauthorized() throws Exception {
    UUID id = UUID.randomUUID();
    mockMvc.perform(patch("/api/v1/notifications/" + id + "/read").with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
