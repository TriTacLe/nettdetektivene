package ntnu.no.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.enums.TaskType;
import ntnu.no.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAttemptVerifierTest {

  @Mock
  TaskRepository taskRepository;

  private final ObjectMapper mapper = new ObjectMapper();
  private TaskAttemptVerifier verifier;
  private UUID taskId;

  @BeforeEach
  void setUp() {
    verifier = new TaskAttemptVerifier(taskRepository, mapper);
    taskId = UUID.randomUUID();
  }

  @Test
  void isCorrect_throws_whenTaskMissing() {
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> verifier.isCorrect(taskId, mapper.createObjectNode()))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void isCorrect_throws_whenAnswersNull() {
    assertThatThrownBy(() -> verifier.isCorrect(taskId, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void news_returnsTrue_whenSelectedArticleIsReal() {
    stubTask(TaskType.NEWS, newsContent(true, false));
    JsonNode answers = mapper.createObjectNode().put("selectedIndex", 0);
    assertThat(verifier.isCorrect(taskId, answers)).isTrue();
  }

  @Test
  void news_returnsFalse_whenSelectedArticleIsFake() {
    stubTask(TaskType.NEWS, newsContent(true, false));
    JsonNode answers = mapper.createObjectNode().put("selectedIndex", 1);
    assertThat(verifier.isCorrect(taskId, answers)).isFalse();
  }

  @Test
  void news_throws_whenIndexOutOfRange() {
    stubTask(TaskType.NEWS, newsContent(true, false));
    JsonNode answers = mapper.createObjectNode().put("selectedIndex", 9);
    assertThatThrownBy(() -> verifier.isCorrect(taskId, answers))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void news_throws_whenSelectedIndexMissing() {
    stubTask(TaskType.NEWS, newsContent(true, false));
    assertThatThrownBy(() -> verifier.isCorrect(taskId, mapper.createObjectNode()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void photo_returnsTrue_whenAllTypesMatch() {
    stubTask(TaskType.PHOTO, photoContent());
    JsonNode answers = photoAnswers(new String[]{"ekte", "ai-generert", "manipulert"});
    assertThat(verifier.isCorrect(taskId, answers)).isTrue();
  }

  @Test
  void photo_returnsFalse_whenAnyTypeWrong() {
    stubTask(TaskType.PHOTO, photoContent());
    JsonNode answers = photoAnswers(new String[]{"ekte", "manipulert", "manipulert"});
    assertThat(verifier.isCorrect(taskId, answers)).isFalse();
  }

  @Test
  void photo_throws_whenSelectionsLengthMismatched() {
    stubTask(TaskType.PHOTO, photoContent());
    JsonNode answers = photoAnswers(new String[]{"ekte", "ai-generert"});
    assertThatThrownBy(() -> verifier.isCorrect(taskId, answers))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void photo_throws_whenSelectionsMissing() {
    stubTask(TaskType.PHOTO, photoContent());
    assertThatThrownBy(() -> verifier.isCorrect(taskId, mapper.createObjectNode()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isCorrect_throws_whenTaskTypeUnsupported() {
    stubTask(TaskType.EMAIL, "{}");
    assertThatThrownBy(() -> verifier.isCorrect(taskId, mapper.createObjectNode()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Verification not implemented");
  }

  private void stubTask(TaskType type, String content) {
    Task task = Task.builder()
        .title("t")
        .taskType(type)
        .content(content)
        .difficulty(1)
        .isActive(true)
        .build();
    when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(task));
  }

  private String newsContent(boolean firstReal, boolean secondReal) {
    ObjectNode root = mapper.createObjectNode();
    ArrayNode articles = root.putArray("articles");
    articles.addObject().put("imagePath", "a").put("isReal", firstReal);
    articles.addObject().put("imagePath", "b").put("isReal", secondReal);
    return root.toString();
  }

  private String photoContent() {
    ObjectNode root = mapper.createObjectNode();
    ArrayNode alts = root.putArray("alternatives");
    addPhotoAlt(alts, "ekte");
    addPhotoAlt(alts, "ai-generert");
    addPhotoAlt(alts, "manipulert");
    return root.toString();
  }

  private void addPhotoAlt(ArrayNode alts, String correctType) {
    ObjectNode alt = alts.addObject();
    alt.put("imagePath", "p");
    alt.put("correctType", correctType);
  }

  private JsonNode photoAnswers(String[] types) {
    ObjectNode root = mapper.createObjectNode();
    ArrayNode selections = root.putArray("selections");
    for (String type : types) {
      ObjectNode sel = selections.addObject();
      sel.put("type", type);
    }
    return root;
  }
}
