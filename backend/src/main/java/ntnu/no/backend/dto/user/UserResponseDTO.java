package ntnu.no.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for returning safe user information.
 * Excludes sensitive fields such as password hash.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response DTO representing a user account")
public class UserResponseDTO {

  /**
   * The unique identifier of a user
   */
  @Schema(description = "Unique identifier of the user")
  private UUID id;

  /**
   * The name display to other users in the application
   */
  @Schema(description = "Display name of the user", example = "Firstname Lastname")
  private String displayName;

  /**
   * The email address of the user
   */
  @Schema(description = "Email address of the user", example = "example@gmail.com")
  private String email;

  /**
   * Whether the account is active or not
   */
  @Schema(description = "Whether the account is active")
  private boolean isActive;

  /**
   * The roles of the user
   */
  @Schema(description = "Roles assigned to the user", example = "student")
  private List<String> roles;

  /**
   * The time the user was created.
   */
  @Schema(description = "Timestamp of account creation")
  private LocalDateTime createdAt;
}
