package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Data Transfer Object for a refreshed token
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data Transfer Object request body for the refresh token")
public class RefreshRequestDTO {
  @NotBlank
  @Schema(description = "The refresh token issued at login")
  private String refreshToken;
}
