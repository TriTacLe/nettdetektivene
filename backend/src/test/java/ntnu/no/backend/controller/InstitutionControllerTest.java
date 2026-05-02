package ntnu.no.backend.controller;

import ntnu.no.backend.security.JWTUtil;
import ntnu.no.backend.service.InstitutionService;
import ntnu.no.backend.util.InstitutionTestFactory;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link InstitutionController}.
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
  void getAllInstitutions_unauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/institutions"))
        .andExpect(status().isUnauthorized());
  }
}
