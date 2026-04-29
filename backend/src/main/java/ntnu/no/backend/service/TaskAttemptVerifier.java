package ntnu.no.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.enums.TaskType;
import ntnu.no.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Verifies that a student's submitted answers match the task's correct
 * answers. Authoritative source of truth for task correctness — never
 * trust a client-supplied verdict.
 *
 * <p>Returns a plain pass/fail. The {@code TaskAttempt} row still carries
 * a numeric {@code score} in the database; callers map the boolean to
 * {@code 100/0} when persisting. Add a new branch here when introducing
 * a new {@link TaskType}.</p>
 */
@Service
@RequiredArgsConstructor
public class TaskAttemptVerifier {

  private final TaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  /**
   * Loads the task and returns whether the supplied answers match.
   *
   * @param taskId  the task being verified
   * @param answers raw student answers (shape depends on task type)
   * @return {@code true} iff every answer matches the task's correct answers
   * @throws IllegalArgumentException for malformed/missing answers
   * @throws EntityNotFoundException  if the task does not exist
   */
  public boolean isCorrect(UUID taskId, JsonNode answers) {
    if (answers == null || answers.isNull()) {
      throw new IllegalArgumentException("Answers are required");
    }
    Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
        .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

    JsonNode content = parseContent(task.getContent());
    return switch (task.getTaskType()) {
      case NEWS -> verifyNews(content, answers);
      case PHOTO -> verifyPhoto(content, answers);
      default -> throw new IllegalArgumentException(
          "Verification not implemented for task type: " + task.getTaskType());
    };
  }

  boolean verifyNews(JsonNode content, JsonNode answers) {
    JsonNode articles = content.path("articles");
    if (!articles.isArray() || articles.isEmpty()) {
      throw new IllegalArgumentException("Task content has no articles");
    }
    JsonNode selectedNode = answers.path("selectedIndex");
    if (!selectedNode.isInt()) {
      throw new IllegalArgumentException("answers.selectedIndex must be an integer");
    }
    int selected = selectedNode.asInt();
    if (selected < 0 || selected >= articles.size()) {
      throw new IllegalArgumentException("answers.selectedIndex out of range");
    }
    return articles.get(selected).path("isReal").asBoolean(false);
  }

  boolean verifyPhoto(JsonNode content, JsonNode answers) {
    JsonNode alternatives = content.path("alternatives");
    if (!alternatives.isArray() || alternatives.isEmpty()) {
      throw new IllegalArgumentException("Task content has no alternatives");
    }
    JsonNode selections = answers.path("selections");
    if (!selections.isArray()) {
      throw new IllegalArgumentException("answers.selections must be an array");
    }
    if (selections.size() != alternatives.size()) {
      throw new IllegalArgumentException(
          "answers.selections length must match alternatives length");
    }
    for (int i = 0; i < alternatives.size(); i++) {
      JsonNode alt = alternatives.get(i);
      JsonNode pick = selections.get(i);
      String correctType = alt.path("correctType").asText("");
      String pickedType = pick.path("type").asText("");
      if (correctType.isEmpty() || !correctType.equals(pickedType)) {
        return false;
      }
    }
    return true;
  }

  private JsonNode parseContent(String content) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Task content is empty");
    }
    try {
      return objectMapper.readTree(content);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Task content is not valid JSON", e);
    }
  }
}
