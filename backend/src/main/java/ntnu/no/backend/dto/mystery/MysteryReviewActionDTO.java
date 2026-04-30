package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ntnu.no.backend.model.enums.MysteryStatus;

/**
 * Request body for the teacher review action — approve or reject a submission.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for reviewing a mystery submission")
public class MysteryReviewActionDTO {

  @NotNull(message = "Status is required")
  @Schema(description = "Review status (APPROVED or REJECTED)")
  private MysteryStatus mysStatus;
}
