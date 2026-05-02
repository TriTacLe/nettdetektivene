package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.achievement.AchievementResponseDTO;
import ntnu.no.backend.model.Achievement;

/**
 * Utility class for mapping between {@link Achievement} entities and achievement-related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see Achievement
 * @see AchievementResponseDTO
 */
public class AchievementMapper {

  private AchievementMapper() {}

  /**
   * Converts an {@link Achievement} entity to an {@link AchievementResponseDTO}.
   *
   * @param achievement the achievement entity to convert
   * @return an {@link AchievementResponseDTO}, or {@code null} if {@code achievement} is null
   */
  public static AchievementResponseDTO toDTO(Achievement achievement) {
    if (achievement == null) return null;
    return new AchievementResponseDTO(
      achievement.getId(),
      achievement.getTitle(),
      achievement.getDescAch(),
      achievement.getIcon(),
      achievement.getTriggerType(),
      achievement.getTriggerRef(),
      achievement.getCreatedAt()
    );
  }
}
