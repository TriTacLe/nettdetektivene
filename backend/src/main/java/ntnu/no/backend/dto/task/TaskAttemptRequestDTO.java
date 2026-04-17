package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting a task attempt.
 * The authenticated user is resolved from the JWT token — do not include userId.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for submitting a task attempt")
public class TaskAttemptRequestDTO {

  @NotNull(message = "Task ID is required")
  @Schema(description = "ID of the task being attempted", example = "3f6e2a1b-...")
  private UUID taskId;

  @NotNull(message = "Classroom ID is required")
  @Schema(description = "ID of the classroom context", example = "7a2b3c4d-...")
  private UUID classroomId;

  @Min(value = 0, message = "Score cannot be negative")
  @Schema(description = "Score achieved on this attempt", example = "80")
  private int score;
}
