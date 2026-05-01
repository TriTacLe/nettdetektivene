package ntnu.no.backend.dto.notebook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a notebook entry.
 *
 * @author tri, norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for creating or updating a notebook entry")
public class NotebookEntryRequestDTO {

  @NotBlank(message = "Stop location is required")
  @Schema(description = "Stop location this entry belongs to", example = "stop_3")
  private String stopLocation;

  @Schema(description = "Auto-generated notes in JSON format")
  private String autoNotes;

  @Size(max = 10000, message = "Reflection cannot exceed 10 000 characters")
  @Schema(description = "Student's personal reflection on the stop")
  private String studentReflection;
}
