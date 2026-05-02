package ntnu.no.backend.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a user profile.
 *
 * @param id unique identifier of the profile
 * @param displayName display name of the user
 * @param feideId FEIDE identifier, {@code null} for non-FEIDE users
 * @param avatarConfig avatar configuration as a JSON string, {@code null} if not customised
 * @param xp total experience points earned
 * @param xpLevel current XP level
 * @param createdAt timestamp of profile creation
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Response DTO representing a user profile")
public record UserProfileResponseDTO(
    UUID id,
    String displayName,
    String feideId,
    String avatarConfig,
    int xp,
    int xpLevel,
    LocalDateTime createdAt,
    String nameChangeStatus
) {}
