package ntnu.no.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.InstitutionService;
import ntnu.no.backend.util.InstitutionTestFactory;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link InstitutionController}.
 * Verifies HTTP status codes and JSON structure.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@WebMvcTest(InstitutionController.class)
@Import(TestSecurityConfig.class)
class InstitutionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InstitutionService institutionService;

  @MockitoBean
  private JWTUtil jwtUtil;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser
  void getAllInstitutions_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(institutionService.getAllInstitutions()).thenReturn(List.of(InstitutionTestFactory.responseDTO(id)));

    mockMvc.perform(get("/api/v1/institutions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].instName").value("NTNU"));
  }

  @Test
  @WithMockUser
  void getInstitutionById_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(institutionService.getInstitutionById(id)).thenReturn(InstitutionTestFactory.responseDTO(id));

    mockMvc.perform(get("/api/v1/institutions/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.instName").value("NTNU"));
  }

  @Test
  @WithMockUser(authorities = "sys_admin")
  void createInstitution_returnsCreated() throws Exception {
    when(institutionService.createInstitution(any())).thenReturn(InstitutionTestFactory.responseDTO(UUID.randomUUID()));

    mockMvc.perform(post("/api/v1/institutions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(InstitutionTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.instName").value("NTNU"));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void createInstitution_asInstructor_returnsForbidden() throws Exception {
    mockMvc.perform(post("/api/v1/institutions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(InstitutionTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "sys_admin")
  void updateInstitution_returnsOk() throws Exception {
    UUID id = UUID.randomUUID();
    when(institutionService.updateInstitution(any(), any())).thenReturn(InstitutionTestFactory.responseDTO(id));

    mockMvc.perform(put("/api/v1/institutions/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(InstitutionTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.instName").value("NTNU"));
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void updateInstitution_asInstructor_returnsForbidden() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(put("/api/v1/institutions/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(InstitutionTestFactory.requestDTO()))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "sys_admin")
  void deleteInstitution_returnsNoContent() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(institutionService).deleteInstitution(id);

    mockMvc.perform(delete("/api/v1/institutions/" + id).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = "instructor")
  void deleteInstitution_asInstructor_returnsForbidden() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/institutions/" + id).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAllInstitutions_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/institutions"))
        .andExpect(status().isUnauthorized());
  }
}
