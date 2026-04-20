package ntnu.no.backend.util;

import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.NotebookEntry;
import ntnu.no.backend.model.auth.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotebookTestFactory {

  public static NotebookEntryRequestDTO requestDTO() {
    return NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .autoNotes("{\"hint\": \"Check the sender address\"}")
        .studentReflection("I learned to identify phishing emails by checking the domain.")
        .build();
  }

  public static NotebookEntryResponseDTO responseDTO(UUID id, UUID userId, UUID classroomId) {
    return new NotebookEntryResponseDTO(
        id, userId, classroomId,
        "stop_3",
        "{\"hint\": \"Check the sender address\"}",
        "I learned to identify phishing emails by checking the domain.",
        LocalDateTime.now(), LocalDateTime.now()
    );
  }

  public static NotebookEntry entity(UUID entryId, User user, Classroom classroom) {
    return NotebookEntry.builder()
        .id(entryId)
        .user(user)
        .classroom(classroom)
        .stopLocation("stop_3")
        .autoNotes("{\"hint\": \"Check the sender address\"}")
        .studentReflection("I learned to identify phishing emails by checking the domain.")
        .build();
  }
}
