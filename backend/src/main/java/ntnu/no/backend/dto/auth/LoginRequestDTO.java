package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Data Transfer Object for user login.
 * Contains the user's email and password for authentication.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data Transfer Object request body for user login")
public class LoginRequestDTO {

  /**
   * The email address used to identify the user account.
   */
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Schema(description = "User email", example = "example@gmail.com")
  private String email;

  /**
   * The user's password for authentication.
   */
  @NotBlank(message = "Password is required")
  @Schema(description = "User password", example = "password123")
  private String password;
}
