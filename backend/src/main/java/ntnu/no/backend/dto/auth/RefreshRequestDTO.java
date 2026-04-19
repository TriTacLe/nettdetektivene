package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Data Transfer Object for token refresh and logout.
 * Contains the raw refresh token issued at login.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body containing the refresh token for session renewal or logout")
public class RefreshRequestDTO {

  /**
   * The raw refresh token issued at login. Used to obtain a new access token or revoke the session.
   */
  @NotBlank
  @Schema(description = "The refresh token issued at login")
  private String refreshToken;

  /**
   * The expired JWT access token. Used to forward claims {@code feideId} to the new token.
   * Optional field, absent for local credential users.
   */
  @Schema(description = "The expired JWT access token, used to carry forward provider claims")
  private String expiredToken;
}
