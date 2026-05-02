package ntnu.no.backend.util;

import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.enums.TaskType;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskTestFactory {

  public static Task task() {
    return Task.builder()
      .title("Spot the phishing email")
      .taskType(TaskType.EMAIL)
      .difficulty(1)
      .content("{}")
      .build();
  }

  public static TaskRequestDTO requestDTO() {
    return TaskRequestDTO.builder()
      .title("Spot the phishing email")
      .taskType(TaskType.EMAIL)
      .difficulty(1)
      .content("{}")
      .build();
  }

  public static TaskRequestDTO requestDTOWithUploadReference() {
    return TaskRequestDTO.builder()
      .title("Spot the phishing email")
      .taskType(TaskType.NEWS)
      .difficulty(1)
      .content("{\"question\":\"Which article is real?\",\"articles\":[{\"imagePath\":\"upload://real-article-1.png\",\"isReal\":true}]}")
      .build();
  }

  public static TaskResponseDTO responseDTO(UUID id) {
    return new TaskResponseDTO(
      id, "Spot the phishing email", TaskType.EMAIL,
      null, 1, "{}", true, LocalDateTime.now(), LocalDateTime.now()
    );
  }
}
