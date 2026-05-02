package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating or updating a classroom.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for creating or updating a classroom")
public class ClassroomRequestDTO {

  @NotBlank(message = "Title is required")
  @Schema(description = "Title of the classroom", example = "Cybersecurity 101")
  private String title;

  @Schema(description = "Description of the classroom", example = "Introduction to cybersecurity")
  private String classDesc;

  @NotNull(message = "Institution ID is required")
  @Schema(description = "ID of the institution this classroom belongs to")
  private UUID institutionId;

  @Schema(description = "Whether the game mode is enabled", example = "false")
  private boolean isGameEnabled;
}
