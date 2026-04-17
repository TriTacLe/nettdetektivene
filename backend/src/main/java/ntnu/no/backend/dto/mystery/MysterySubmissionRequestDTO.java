package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting a mystery.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for submitting a mystery")
public class MysterySubmissionRequestDTO {

  @NotBlank(message = "Description is required")
  @Schema(description = "Description of the mystery", example = "I found a suspicious email asking for my password")
  private String mysDesc;

  @Schema(description = "Optional URL to an attachment supporting the submission")
  private String attachmentUrl;
}
