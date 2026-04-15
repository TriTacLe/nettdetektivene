package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author norakirknes
 * @author tri :)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object request data for registering a user")
public class RegisterRequestDTO{

    @NotBlank(message = "Display name is required")
    @Schema(description = "Display name", example = "Firstname Lastname")
    private String displayName;

    /**
     * The email of the user, must be in accepted format and cannot be blank
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User email", example = "example@gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters with letters and numbers")
    @Schema(description = "Password", example = "password123")
    private String password;
}