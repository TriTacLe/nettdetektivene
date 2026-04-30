package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body when a student picks an alternative for a mystery.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "The alternative a student selected for a mystery")
public class MysteryCompletionRequestDTO {

  @NotNull(message = "Selected index is required")
  @Min(value = 0, message = "Selected index must be 0, 1 or 2")
  @Max(value = 2, message = "Selected index must be 0, 1 or 2")
  @Schema(description = "Index (0, 1 or 2) of the alternative the student chose")
  private Byte selectedIndex;
}
