package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating task assignment settings inside a classroom.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for updating a classroom task assignment")
public class ClassroomTaskUpdateRequestDTO {

  @Schema(description = "Whether the task is active in this classroom", example = "true")
  private Boolean isActive;

  @Schema(description = "Display order inside the classroom", example = "1")
  private Short sortOrder;
}