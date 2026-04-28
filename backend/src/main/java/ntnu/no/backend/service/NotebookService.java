package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.controller.NotebookController;
import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.mapper.NotebookEntryMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.model.NotebookEntry;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.NotebookEntryRepository;
import ntnu.no.backend.repository.NotificationRepository;
import ntnu.no.backend.repository.UserRepository;

import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.shared.CensoredWords;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing notebook entries.
 *
 * @author tri, norakirknes
 * @version 0.2
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
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final NotificationRepository notificationRepository;

  /**
   * Creates a new notebook entry for a student in a classroom.
   * Verifies that the student is an approved member of the classroom.
   *
   * @param classroomId the classroom ID
   * @param dto the notebook entry data
   * @param userId the ID of the student creating the entry
   * @return the created notebook entry
   * @throws EntityNotFoundException if the classroom or user is not found
   * @throws AccessDeniedException if the student is not an approved member
   * @see ClassroomRepository#findByIdAndDeletedAtIsNull(UUID)
   */
  @Transactional(noRollbackFor = IllegalArgumentException.class)
  public NotebookEntryResponseDTO createNotebook(UUID classroomId, NotebookEntryRequestDTO dto, UUID userId) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(classroomId)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + classroomId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(classroomId, user);
    validateContent(dto, classroom, user);

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
   * Verifies that the authenticated user owns the notebook entry.
   *
   * @param dto the updated notebook entry data
   * @param id the entry ID
   * @param userId the ID of the user attempting to update
   * @return the updated notebook entry
   * @throws EntityNotFoundException if the entry is not found
   * @throws AccessDeniedException if the user does not own the notebook entry
   * @see #createNotebook(UUID, NotebookEntryRequestDTO, UUID)
   */
  @Transactional(noRollbackFor = IllegalArgumentException.class)
  public NotebookEntryResponseDTO updateNotebook(NotebookEntryRequestDTO dto, UUID id, UUID userId) {
    NotebookEntry notebook = notebookEntryRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Notebook not found with id: " + id));

    verifyNotebookOwnership(notebook, userId);
    validateContent(dto, notebook.getClassroom(), notebook.getUser());

    notebook.setAutoNotes(dto.getAutoNotes());
    notebook.setStopLocation(dto.getStopLocation());
    notebook.setStudentReflection(dto.getStudentReflection());

    return NotebookEntryMapper.toDTO(notebookEntryRepository.save(notebook));
  }

  /**
   * Deletes a notebook entry.
   * Verifies that the authenticated user owns the notebook entry.
   *
   * @param id the entry ID
   * @param userId the ID of the user attempting to delete
   * @throws EntityNotFoundException if the entry is not found
   * @throws AccessDeniedException if the user does not own the notebook entry
   */
  @Transactional
  public void deleteNotebook(UUID id, UUID userId) {
    NotebookEntry notebook = notebookEntryRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Notebook not found with id: " + id));

    verifyNotebookOwnership(notebook, userId);

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

  /**
   * Verifies that the student is an approved member of the classroom.
   *
   * @param classroomId the classroom ID
   * @param user the student to verify
   * @throws AccessDeniedException if the student is not an approved member
   */
  private void verifyStudentMembership(UUID classroomId, User user) {
    ClassroomStudent membership = classroomStudentRepository.findById(
        ClassroomStudentId.builder()
            .classroomId(classroomId)
            .userId(user.getId())
            .build()
    ).orElse(null);

    if (membership == null || !membership.isAllowed()) {
      throw new AccessDeniedException("You are not an approved member of this classroom");
    }
  }

  /**
   * Validates the contents the student is trying to save.
   * Checks if the entry contains any of the predefined curse words.
   * If the entry contains a curse word a notification is sent to all
   * instructors of the classroom
   *
   * @param dto the notebook entry request
   * @param classroom the classroom of the student
   * @param user the user (student) making the notebook entry.
   * @throws IllegalArgumentException if the contents has inappropriate words
   */
  private void validateContent(NotebookEntryRequestDTO dto, Classroom classroom, User user) {
    if (CensoredWords.containsBlockedWord(dto.getStudentReflection())) {
      classroomInstructorRepository.findAllByClassroom(classroom)
          .forEach(instructor -> notificationRepository.save(
              Notification.builder()
                  .user(instructor.getUser())
                  .classroom(classroom)
                  .msg(user.getDisplayName() + " skrev upassende tekst i notatblokken")
                  .refType("inappropriate_message")
                  .refId(user.getId())
                  .isRead(false)
                  .build()
          ));
      throw new IllegalArgumentException("Teksten inneholder upassende ord");
    }
  }

  /**
   * Verifies that the user owns the notebook entry.
   *
   * @param notebook the notebook entry to verify
   * @param userId the ID of the user attempting access
   * @throws AccessDeniedException if the user does not own the notebook entry
   */
  private void verifyNotebookOwnership(NotebookEntry notebook, UUID userId) {
    if (!notebook.getUser().getId().equals(userId)) {
      throw new AccessDeniedException("You do not own this notebook entry");
    }
  }
}
