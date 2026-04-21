package ntnu.no.backend.service;

import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.repository.InstitutionRepository;
import ntnu.no.backend.util.InstitutionTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstitutionService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

  @Mock
  private InstitutionRepository institutionRepository;

  @InjectMocks
  private InstitutionService institutionService;

  @Test
  void getAllInstitutions_returnsMappedDTOs() {
    when(institutionRepository.findAll()).thenReturn(List.of(InstitutionTestFactory.institution()));

    List<InstitutionResponseDTO> result = institutionService.getAllInstitutions();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().instName()).isEqualTo("NTNU");
  }

  @Test
  void getAllInstitutions_returnsEmptyList_whenNoneExist() {
    when(institutionRepository.findAll()).thenReturn(List.of());

    List<InstitutionResponseDTO> result = institutionService.getAllInstitutions();

    assertThat(result).isEmpty();
  }
}
