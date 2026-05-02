package ntnu.no.backend.dto.achievement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing an achievement that can be earned by a student.
 *
 * @param id unique identifier of the achievement
 * @param title title of the achievement
 * @param descAch description of what the achievement represents or how to earn it
 * @param icon icon identifier for the achievement's visual representation
 * @param triggerType type of event that triggers this achievement
 * @param triggerRef optional reference value used alongside triggerType
 * @param createdAt timestamp of when this achievement was created
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents an achievement that can be earned by a student user")
public record AchievementResponseDTO(
    UUID id,
    String title,
    String descAch,
    String icon,
    String triggerType,
    String triggerRef,
    LocalDateTime createdAt
) {}
