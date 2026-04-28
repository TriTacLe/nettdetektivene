package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting a mystery.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for submitting a mystery")
public class MysterySubmissionRequestDTO {

  @NotNull(message = "Classroom id is required")
  @Schema(description = "Unique identifier of the classroom", example = "11111111-1111-1111-1111-111111111111")
  private UUID classroomId;

  @NotBlank(message = "Description is required")
  @Schema(description = "Description of the mystery", example = "I found a suspicious email asking for my password")
  private String mysDesc;

  @Schema(description = "Optional URL to an attachment supporting the submission")
  private String attachmentUrl;
}
