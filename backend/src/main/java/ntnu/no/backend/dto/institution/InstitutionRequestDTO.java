package ntnu.no.backend.dto.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating an institution.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload for creating or updating an institution")
public class InstitutionRequestDTO {

  @NotBlank(message = "Institution name is required")
  @Schema(description = "Name of the institution", example = "NTNU")
  private String instName;
}
