package ntnu.no.backend.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response Data Transfer Object for returning user profile information.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response DTO representing a user profile")
public class UserProfileResponseDTO {

  /**
   * The unique identifier of the profile.
   */
  @Schema(description = "Unique identifier of the profile")
  private UUID id;

  /**
   * The name displayed to other users in the application.
   */
  @Schema(description = "Display name of the user", example = "Firstname Lastname")
  private String displayName;

  /**
   * The FEIDE identifier for this profile, {@code null} for non-FEIDE users.
   */
  @Schema(description = "FEIDE identifier, null for non-FEIDE users")
  private String feideId;

  /**
   * JSON blob for the user's avatar configuration.
   * {@code null} if the user has not customised their avatar.
   */
  @Schema(description = "Avatar configuration as a JSON string")
  private String avatarConfig;

  /**
   * Total experience points earned by the user.
   */
  @Schema(description = "Total experience points", example = "120")
  private int xp;

  /**
   * The user's current XP level.
   */
  @Schema(description = "Current XP level", example = "3")
  private int xpLevel;

  /**
   * Timestamp of when the profile was created.
   */
  @Schema(description = "Timestamp of profile creation")
  private LocalDateTime createdAt;
}
