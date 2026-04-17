package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ntnu.no.backend.model.enums.TaskType;

/**
 * Request DTO for creating or updating a task.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for creating or updating a task")
public class TaskRequestDTO {

  @NotBlank(message = "Title is required")
  @Schema(description = "Title of the task", example = "Spot the phishing email")
  private String title;

  @NotNull(message = "Task type is required")
  @Schema(description = "Category of the task", example = "EMAIL")
  private TaskType taskType;

  @Schema(description = "Stop location identifier for the task", example = "stop_3")
  private String stopLocation;

  @Min(value = 1, message = "Difficulty must be between 1 and 5")
  @Max(value = 5, message = "Difficulty must be between 1 and 5")
  @Schema(description = "Difficulty level from 1 to 5", example = "2")
  private int difficulty = 1;

  @NotBlank(message = "Content is required")
  @Schema(description = "JSON content of the task")
  private String content;
}
