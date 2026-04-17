package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.InstitutionRepository;
import ntnu.no.backend.util.ClassroomTestFactory;
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
 * Unit tests for {@link ClassroomService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

  @Mock
  private ClassroomRepository classroomRepository;

  @Mock
  private InstitutionRepository institutionRepository;

  @InjectMocks
  private ClassroomService classroomService;

  @Test
  void getAllClassroom_returnsMappedDTOs() {
    when(classroomRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(ClassroomTestFactory.classroom()));

    List<ClassroomResponseDTO> result = classroomService.getAllClassroom();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void getClassroom_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(ClassroomTestFactory.classroom()));

    ClassroomResponseDTO result = classroomService.getClassroom(id);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void getClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.getClassroom(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void createClassroom_savesAndReturnsDTO() {
    User currentUser = ClassroomTestFactory.user();
    when(institutionRepository.findById(any())).thenReturn(Optional.of(ClassroomTestFactory.institution()));
    when(classroomRepository.save(any(Classroom.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomResponseDTO result = classroomService.createClassroom(ClassroomTestFactory.requestDTO(), currentUser);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
    verify(classroomRepository).save(any(Classroom.class));
  }

  @Test
  void createClassroom_throwsEntityNotFoundException_whenInstitutionNotFound() {
    when(institutionRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.createClassroom(ClassroomTestFactory.requestDTO(), ClassroomTestFactory.user()))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateClassroom_updatesAndReturnsDTO() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(ClassroomTestFactory.classroom()));

    ClassroomResponseDTO result = classroomService.updateClassroom(
      ClassroomTestFactory.requestDTO(), id);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void updateClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.updateClassroom(ClassroomTestFactory.requestDTO(), id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void deleteClassroom_softDeletes_whenFound() {
    UUID id = UUID.randomUUID();
    Classroom classroom = ClassroomTestFactory.classroom();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(classroom));

    classroomService.deleteClassroom(id);

    assertThat(classroom.getDeletedAt()).isNotNull();
    assertThat(classroom.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void deleteClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.deleteClassroom(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void getAllClassroomsByInstitution_returnsMappedDTOs() {
    UUID institutionId = UUID.randomUUID();
    when(classroomRepository.findAllByInstitution_IdAndDeletedAtIsNull(institutionId))
      .thenReturn(List.of(ClassroomTestFactory.classroom()));

    List<ClassroomResponseDTO> result = classroomService.getAllClassroomsByInstitution(institutionId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Cybersecurity 101");
  }
}
