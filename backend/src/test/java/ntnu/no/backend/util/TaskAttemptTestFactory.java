package ntnu.no.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ntnu.no.backend.dto.task.TaskAttemptRequestDTO;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.TaskAttempt;
import ntnu.no.backend.model.auth.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskAttemptTestFactory {

  public static User user() {
    User user = User.builder()
        .displayName("Anine")
        .email("anine@ntnu.no")
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }

  public static Task task() {
    return TaskTestFactory.task();
  }

  public static Classroom classroom() {
    return ClassroomTestFactory.classroom();
  }

  public static TaskAttempt attempt(User user, Task task, Classroom classroom) {
    return TaskAttempt.builder()
        .user(user)
        .task(task)
        .classroom(classroom)
        .score(85)
        .passed(true)
        .build();
  }

  public static TaskAttemptRequestDTO requestDTO(UUID taskId, UUID classroomId) {
    return requestDTO(taskId, classroomId, defaultAnswers());
  }

  public static TaskAttemptRequestDTO requestDTO(UUID taskId, UUID classroomId, JsonNode answers) {
    return TaskAttemptRequestDTO.builder()
        .taskId(taskId)
        .classroomId(classroomId)
        .answers(answers)
        .build();
  }

  public static JsonNode defaultAnswers() {
    ObjectNode node = new ObjectMapper().createObjectNode();
    node.put("selectedIndex", 0);
    return node;
  }

  public static TaskAttemptResponseDTO responseDTO(UUID id) {
    return new TaskAttemptResponseDTO(
        id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        85, true, LocalDateTime.now()
    );
  }
}
