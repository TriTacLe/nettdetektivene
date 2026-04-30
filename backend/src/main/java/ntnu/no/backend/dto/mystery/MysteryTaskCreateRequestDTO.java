package ntnu.no.backend.dto.mystery;

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
 * Request DTO for turning a mystery submission into a classroom task.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for creating a task from a mystery submission")
public class MysteryTaskCreateRequestDTO {

  @NotBlank(message = "Title is required")
  @Schema(description = "Title of the generated task", example = "Undersøk den falske nyheten")
  private String title;

  @NotNull(message = "Task type is required")
  @Schema(description = "Task category", example = "NEWS")
  private TaskType taskType;

  @Schema(description = "Stop location identifier", example = "nyhetskvartalet")
  private String stopLocation;

  @Min(value = 1, message = "Difficulty must be between 1 and 5")
  @Max(value = 5, message = "Difficulty must be between 1 and 5")
  @Schema(description = "Difficulty level from 1 to 5", example = "2")
  private int difficulty = 1;

  @Schema(description = "Optional JSON content. If omitted, content is generated from the submission.")
  private String content;

  @Schema(description = "Display order inside the classroom. Defaults to the end of the list.", example = "5")
  private Short sortOrder;

  @Schema(description = "Whether the generated classroom task should be active", example = "true")
  private Boolean isActive;
}
