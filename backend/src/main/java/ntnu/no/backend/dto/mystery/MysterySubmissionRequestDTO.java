package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for submitting a mystery. Sent as a JSON part of a
 * multipart request alongside the image file.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for submitting a mystery")
public class MysterySubmissionRequestDTO {

  @NotNull(message = "Classroom id is required")
  @Schema(description = "Unique identifier of the classroom")
  private UUID classroomId;

  @NotBlank(message = "Alternative 1 is required")
  @Size(max = 500, message = "Alternative 1 must be at most 500 characters")
  @Schema(description = "Text of the first alternative answer")
  private String alternative1;

  @NotBlank(message = "Alternative 2 is required")
  @Size(max = 500, message = "Alternative 2 must be at most 500 characters")
  @Schema(description = "Text of the second alternative answer")
  private String alternative2;

  @NotBlank(message = "Alternative 3 is required")
  @Size(max = 500, message = "Alternative 3 must be at most 500 characters")
  @Schema(description = "Text of the third alternative answer")
  private String alternative3;

  @NotNull(message = "Correct index is required")
  @Min(value = 0, message = "Correct index must be 0, 1 or 2")
  @Max(value = 2, message = "Correct index must be 0, 1 or 2")
  @Schema(description = "Index (0, 1 or 2) of the correct alternative")
  private Byte correctIndex;
}
