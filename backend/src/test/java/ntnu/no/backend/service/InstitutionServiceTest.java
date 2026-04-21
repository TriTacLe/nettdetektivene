package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.repository.InstitutionRepository;
import ntnu.no.backend.util.InstitutionTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
    when(institutionRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(InstitutionTestFactory.institution()));

    List<InstitutionResponseDTO> result = institutionService.getAllInstitutions();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).instName()).isEqualTo("NTNU");
  }

  @Test
  void getInstitutionById_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(InstitutionTestFactory.institution()));

    InstitutionResponseDTO result = institutionService.getInstitutionById(id);

    assertThat(result.instName()).isEqualTo("NTNU");
  }

  @Test
  void getInstitutionById_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> institutionService.getInstitutionById(id))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void createInstitution_savesAndReturnsDTO() {
    when(institutionRepository.save(any(Institution.class))).thenAnswer(i -> i.getArgument(0));

    InstitutionResponseDTO result = institutionService.createInstitution(InstitutionTestFactory.requestDTO());

    assertThat(result.instName()).isEqualTo("NTNU");
    verify(institutionRepository).save(any(Institution.class));
  }

  @Test
  void updateInstitution_updatesAndReturnsDTO() {
    UUID id = UUID.randomUUID();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(InstitutionTestFactory.institution()));

    InstitutionResponseDTO result = institutionService.updateInstitution(id, InstitutionTestFactory.requestDTO());

    assertThat(result.instName()).isEqualTo("NTNU");
  }

  @Test
  void updateInstitution_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> institutionService.updateInstitution(id, InstitutionTestFactory.requestDTO()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void deleteInstitution_softDeletes_whenFound() {
    UUID id = UUID.randomUUID();
    Institution institution = InstitutionTestFactory.institution();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(institution));

    institutionService.deleteInstitution(id);

    assertThat(institution.getDeletedAt()).isNotNull();
    assertThat(institution.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void deleteInstitution_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(institutionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> institutionService.deleteInstitution(id))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }
}
