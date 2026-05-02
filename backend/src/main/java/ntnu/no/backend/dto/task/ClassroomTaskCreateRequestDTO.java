package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for assigning a task to a classroom.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for assigning a task to a classroom")
public class ClassroomTaskCreateRequestDTO {

  @NotNull(message = "Task ID is required")
  @Schema(description = "Task to assign to the classroom")
  private UUID taskId;

  @Schema(description = "Display order inside the classroom", example = "1")
  private Short sortOrder;

  @Schema(description = "Whether the task should be active in the classroom", example = "true")
  private Boolean isActive;
}
