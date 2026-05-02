package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.sidequest.SideQuestStateDTO;
import ntnu.no.backend.dto.sidequest.SideQuestUpdateRequestDTO;
import ntnu.no.backend.model.SideQuestStatus;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.SideQuestService;
import ntnu.no.backend.util.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SideQuestController.class)
@Import(TestSecurityConfig.class)
@RequiredArgsConstructor
class SideQuestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private SideQuestService sideQuestService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  private final UUID userId = UUID.randomUUID();

  private User sampleUser(String... roleNames) {
    List<Role> roles = new ArrayList<>();
    for (String roleName : roleNames) {
      Role role = new Role();
      role.setRoleName(roleName);
      roles.add(role);
    }
    return User.builder()
        .id(userId)
        .displayName("Test User")
        .email("test@example.com")
        .roles(roles)
        .build();
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void getMine_returns200_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(sideQuestService.getMine(userId))
        .thenReturn(List.of(new SideQuestStateDTO("hacker", 1, SideQuestStatus.ACTIVE)));

    mockMvc.perform(get("/api/v1/side-quests/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].key").value("hacker"))
        .andExpect(jsonPath("$.data[0].stage").value(1))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
  }

  @Test
  void getMine_returns401_whenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/v1/side-quests/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void upsert_returns200_andEchoesState_whenAuthenticated() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);
    when(sideQuestService.upsert(eq(userId), eq("hacker"), anyInt(), eq(SideQuestStatus.COMPLETED)))
        .thenReturn(new SideQuestStateDTO("hacker", 1, SideQuestStatus.COMPLETED));

    SideQuestUpdateRequestDTO body = new SideQuestUpdateRequestDTO(1, SideQuestStatus.COMPLETED);
    mockMvc.perform(post("/api/v1/side-quests/me/{questKey}", "hacker")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
  }

  @Test
  void upsert_returns401_whenNotAuthenticated() throws Exception {
    SideQuestUpdateRequestDTO body = new SideQuestUpdateRequestDTO(1, SideQuestStatus.ACTIVE);
    mockMvc.perform(post("/api/v1/side-quests/me/{questKey}", "hacker")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void upsert_returns400_whenStageNegative() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);

    SideQuestUpdateRequestDTO body = new SideQuestUpdateRequestDTO(-1, SideQuestStatus.ACTIVE);
    mockMvc.perform(post("/api/v1/side-quests/me/{questKey}", "hacker")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upsert_returns400_whenStageMissing() throws Exception {
    User user = sampleUser("student");
    setAuthenticatedUser(user);

    String body = "{\"status\":\"ACTIVE\"}";
    mockMvc.perform(post("/api/v1/side-quests/me/{questKey}", "hacker")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }
}
