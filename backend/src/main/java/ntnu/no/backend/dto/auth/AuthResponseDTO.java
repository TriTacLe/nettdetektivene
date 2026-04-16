package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data Transfer Object for JWT token after a successful authentication")
public class AuthResponseDTO {
  @Schema(description = "The secret JWT token")
  private String token;

  @Schema(description = "The refresh token for session renewal")
  private String refreshToken;
}