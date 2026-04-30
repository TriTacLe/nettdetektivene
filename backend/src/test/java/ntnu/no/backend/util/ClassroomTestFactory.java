package ntnu.no.backend.util;

import ntnu.no.backend.dto.classroom.AddInstructorRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskCreateRequestDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;

import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.model.enums.NameChangeStatus;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ClassroomTestFactory {

  public static Institution institution() {
    return Institution.builder()
      .instName("NTNU")
      .build();
  }

  public static User user() {
    return User.builder()
      .displayName("Nora")
      .email("nora@ntnu.no")
      .build();
  }

  public static Classroom classroom() {
    return Classroom.builder()
      .title("Cybersecurity 101")
      .classDesc("Introduction to cybersecurity")
      .joinCode("ABC12")
      .institution(institution())
      .createdBy(user())
      .isGameEnabled(false)
      .build();
  }

  public static ClassroomRequestDTO requestDTO() {
    return ClassroomRequestDTO.builder()
      .title("Cybersecurity 101")
      .classDesc("Introduction to cybersecurity")
      .institutionId(UUID.randomUUID())
      .isGameEnabled(false)
      .build();
  }

  public static ClassroomResponseDTO responseDTO(UUID id) {
    return new ClassroomResponseDTO(
      id, "Cybersecurity 101", "Introduction to cybersecurity",
      "ABC12", UUID.randomUUID(), UUID.randomUUID(),
      false, LocalDateTime.now(), LocalDateTime.now()
    );
  }

  public static ClassroomTaskCreateRequestDTO classroomTaskCreateRequestDTO(UUID taskId) {
    return ClassroomTaskCreateRequestDTO.builder()
      .taskId(taskId)
      .sortOrder((short) 1)
      .isActive(true)
      .build();
  }

  public static ClassroomTaskResponseDTO classroomTaskResponseDTO(UUID classroomId, UUID taskId) {
    return new ClassroomTaskResponseDTO(
      classroomId,
      taskId,
      (short) 1,
      true,
      false,
      TaskTestFactory.responseDTO(taskId)
    );
  }

  public static ClassroomTaskResponseDTO classroomTaskResponseDTO(UUID classroomId, UUID taskId, boolean isUnlocked) {
    return new ClassroomTaskResponseDTO(
      classroomId,
      taskId,
      (short) 1,
      true,
      isUnlocked,
      TaskTestFactory.responseDTO(taskId)
    );
  }

  public static Role instructorRole() {
    return Role.builder()
      .roleName("instructor")
      .build();
  }

  public static Role studentRole() {
    return Role.builder()
      .roleName("student")
      .build();
  }

  public static User teacher(String displayName, String email) {
    return User.builder()
      .displayName(displayName)
      .email(email)
      .roles(List.of(instructorRole()))
      .build();
  }

  public static User student(String displayName, String email) {
    return User.builder()
      .displayName(displayName)
      .email(email)
      .roles(List.of(studentRole()))
      .build();
  }

  public static AddInstructorRequestDTO addInstructorRequestDTO(String email) {
    return new AddInstructorRequestDTO(email);
  }

  public static ClassroomInstructorResponseDTO instructorResponseDTO(UUID classroomId, UUID userId) {
    return new ClassroomInstructorResponseDTO(classroomId, userId, LocalDateTime.now());
  }

  public static ClassroomInstructor classroomInstructor(Classroom classroom, User user) {
    return ClassroomInstructor.builder()
      .id(new ClassroomInstructorId(classroom.getId(), user.getId()))
      .classroom(classroom)
      .user(user)
      .assignedAt(LocalDateTime.now())
      .build();

  public static ClassroomStudentResponseDTO classroomStudentResponseDTO(UUID classroomId, UUID userId) {
    return new ClassroomStudentResponseDTO(
      classroomId,
      userId,
      "Nora",
      true,
      NameChangeStatus.NONE,
      null,
      LocalDateTime.now(),
      "nora-feide-123"
    );
  }
}
