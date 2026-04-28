package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ntnu.no.backend.model.enums.MysteryStatus;

/**
 * Request DTO for reviewing a mystery submission.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for reviewing a mystery submission")
public class MysteryReviewDTO {

  @NotNull(message = "Status is required")
  @Schema(description = "Review status", example = "APPROVED")
  private MysteryStatus mysStatus;

  @Schema(description = "Whether this approved submission should be featured")
  private boolean featured;
}
