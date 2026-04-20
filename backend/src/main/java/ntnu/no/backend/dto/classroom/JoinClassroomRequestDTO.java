package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for joining a classroom via join code.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for joining a classroom")
public class JoinClassroomRequestDTO {

  /**
   * The join code of the classroom to join.
   */
  @NotBlank(message = "Join code is required")
  @Schema(description = "The classroom join code", example = "ABC12")
  private String joinCode;
}
