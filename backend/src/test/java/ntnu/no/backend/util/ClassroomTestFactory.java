package ntnu.no.backend.util;

import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.auth.User;

import java.time.LocalDateTime;
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
}
