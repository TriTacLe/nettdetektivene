package ntnu.no.backend.dto.achievement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing an achievement earned by a user.
 * Achievements are awarded automatically by the {@code trg_mystery_approved} trigger.
 *
 * @param userId unique identifier of the user
 * @param achievementId unique identifier of the achievement
 * @param achievementName name/title of the achievement
 * @param classroomId unique identifier of the classroom context, may be null
 * @param earnedAt timestamp of when the achievement was earned
 *
 * @author tri
 * @version 0.2
 * @since 0.1
 */
@Schema(description = "Represents an achievement earned by a user")
public record UserAchievementResponseDTO(
    UUID userId,
    UUID achievementId,
    String achievementName,
    UUID classroomId,
    LocalDateTime earnedAt
) {}
