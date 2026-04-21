package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.controller.NotebookController;
import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.mapper.NotebookEntryMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.NotebookEntry;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.NotebookEntryRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing notebook entries.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see NotebookController
 * @see NotebookEntry
 */
@Service
@RequiredArgsConstructor
public class NotebookService {
  private final NotebookEntryRepository notebookEntryRepository;
  private final ClassroomRepository classroomRepository;
  private final UserRepository userRepository;

  /**
   * Creates a new notebook entry for a student in a classroom.
   *
   * @param classroomId the classroom ID
   * @param dto the notebook entry data
   * @param userId the ID of the student creating the entry
   * @return the created notebook entry
   * @throws EntityNotFoundException if the classroom or user is not found
   * @see ClassroomRepository#findByIdAndDeletedAtIsNull(UUID)
   */
  @Transactional
  public NotebookEntryResponseDTO createNotebook(UUID classroomId, NotebookEntryRequestDTO dto, UUID userId) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(classroomId)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + classroomId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    NotebookEntry notebook = NotebookEntry.builder()
        .user(user)
        .classroom(classroom)
        .stopLocation(dto.getStopLocation())
        .studentReflection(dto.getStudentReflection())
        .autoNotes(dto.getAutoNotes())
        .build();

    return NotebookEntryMapper.toDTO(notebookEntryRepository.save(notebook));
  }

  /**
   * Updates the content of an existing notebook entry.
   *
   * @param dto the updated notebook entry data
   * @param id the entry ID
   * @return the updated notebook entry
   * @throws EntityNotFoundException if the entry is not found
   * @see #createNotebook(UUID, NotebookEntryRequestDTO, UUID)
   */
  @Transactional
  public NotebookEntryResponseDTO updateNotebook(NotebookEntryRequestDTO dto, UUID id) {
    NotebookEntry notebook = notebookEntryRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Notebook not found with id: " + id));

    notebook.setAutoNotes(dto.getAutoNotes());
    notebook.setStopLocation(dto.getStopLocation());
    notebook.setStudentReflection(dto.getStudentReflection());

    return NotebookEntryMapper.toDTO(notebookEntryRepository.save(notebook));
  }

  /**
   * Deletes a notebook entry.
   *
   * @param id the entry ID
   * @throws EntityNotFoundException if the entry is not found
   */
  @Transactional
  public void deleteNotebook(UUID id) {
    NotebookEntry notebook = notebookEntryRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Notebook not found with id: " + id));
    notebookEntryRepository.delete(notebook);
  }

  /**
   * Returns all notebook entries written by a specific student in a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @return list of the student's notebook entries
   */
  @Transactional(readOnly = true)
  public List<NotebookEntryResponseDTO> getNotebookByClassroomAndUser(UUID classroomId, UUID userId) {
    return notebookEntryRepository.findByClassroom_IdAndUser_Id(classroomId, userId)
        .stream().map(NotebookEntryMapper::toDTO).toList();
  }

  /**
   * Returns all notebook entries in a classroom, regardless of which student wrote them.
   *
   * @param classroomId the classroom ID
   * @return list of all notebook entries in the classroom
   */
  @Transactional(readOnly = true)
  public List<NotebookEntryResponseDTO> getNotebooksByClassroom(UUID classroomId) {
    return notebookEntryRepository.findByClassroom_Id(classroomId)
        .stream().map(NotebookEntryMapper::toDTO).toList();
  }
}
