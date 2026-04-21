package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.UserService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link UserController}.
 * Verifies HTTP status codes and JSON structure for user management endpoints.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private User sampleUser(String... roleNames) {
    List<Role> roles = new ArrayList<>();
    for (String roleName : roleNames) {
      Role role = new Role();
      role.setRoleName(roleName);
      roles.add(role);
    }

    return User.builder()
        .id(TEST_USER_ID)
        .displayName("Test User")
        .email("test@example.com")
        .roles(roles)
        .build();
  }


  private UserResponseDTO sampleUserResponse() {
    return UserResponseDTO.builder()
        .id(TEST_USER_ID)
        .displayName("Test User")
        .email("test@example.com")
        .isActive(true)
        .roles(List.of("student"))
        .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
        .build();
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void getUser_returnsOk() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(userService.getUser(TEST_USER_ID)).thenReturn(sampleUserResponse());

    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Test User"))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.roles[0]").value("student"));
  }

  @Test
  void getUser_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateUser_returnsOk() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);

    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    UserResponseDTO updatedResponse = UserResponseDTO.builder()
        .id(TEST_USER_ID)
        .displayName("Updated Name")
        .email("test@example.com")
        .isActive(true)
        .roles(List.of("student"))
        .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
        .build();
    when(userService.updateUser(eq(TEST_USER_ID), any(UserUpdateDTO.class))).thenReturn(updatedResponse);

    mockMvc.perform(patch("/api/v1/users/me")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Updated Name"));
  }

  @Test
  void updateUser_unauthenticated_returnsUnauthorized() throws Exception {
    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    mockMvc.perform(patch("/api/v1/users/me")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateUserById_asInstructor_returnsOk() throws Exception {
    User instructor = sampleUser("instructor");
    setAuthenticatedUser(instructor);

    UUID studentId = UUID.randomUUID();
    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("Updated Student")
        .build();

    UserResponseDTO updatedResponse = UserResponseDTO.builder()
        .id(studentId)
        .displayName("Updated Student")
        .email("student@example.com")
        .isActive(true)
        .roles(List.of("student"))
        .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
        .build();
    when(userService.updateUser(eq(studentId), any(UserUpdateDTO.class))).thenReturn(updatedResponse);

    mockMvc.perform(patch("/api/v1/users/" + studentId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Updated Student"));
  }

  @Test
  void updateUserById_asStudent_returnsForbidden() throws Exception {
    User student = sampleUser("student");
    setAuthenticatedUser(student);

    UUID otherId = UUID.randomUUID();
    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("Should Not Work")
        .build();

    mockMvc.perform(patch("/api/v1/users/" + otherId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateUserById_unauthenticated_returnsUnauthorized() throws Exception {
    UUID otherId = UUID.randomUUID();
    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("Should Not Work")
        .build();

    mockMvc.perform(patch("/api/v1/users/" + otherId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateUser_blankDisplayName_returnsBadRequest() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);

    UserUpdateDTO updateDTO = UserUpdateDTO.builder()
        .displayName("")
        .build();

    mockMvc.perform(patch("/api/v1/users/me")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }
}