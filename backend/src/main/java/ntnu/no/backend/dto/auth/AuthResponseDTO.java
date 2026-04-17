package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response Data Transfer Object returned after successful authentication.
 * Contains a short-lived JWT access token and a long-lived refresh token.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response body containing a JWT access token and a refresh token after successful authentication")
public class AuthResponseDTO {

  /**
   * Short-lived JWT access token used to authenticate requests.
   */
  @Schema(description = "The JWT access token")
  private String token;

  /**
   * Long-lived refresh token used to obtain a new access token when it expires.
   */
  @Schema(description = "The refresh token for session renewal")
  private String refreshToken;
}