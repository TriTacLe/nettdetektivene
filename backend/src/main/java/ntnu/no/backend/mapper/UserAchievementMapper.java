package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.model.UserAchievement;

/**
 * Utility class for mapping between {@link UserAchievement} entities and related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see UserAchievement
 * @see UserAchievementResponseDTO
 */
public class UserAchievementMapper {

  private UserAchievementMapper() {}

  /**
   * Converts a {@link UserAchievement} entity to a {@link UserAchievementResponseDTO}.
   *
   * @param userAchievement the user achievement entity to convert
   * @return a {@link UserAchievementResponseDTO}, or {@code null} if {@code userAchievement} is null
   */
  public static UserAchievementResponseDTO toDTO(UserAchievement userAchievement) {
    if (userAchievement == null) return null;
    return new UserAchievementResponseDTO(
      userAchievement.getUser().getId(),
      userAchievement.getAchievement().getId(),
      userAchievement.getClassroom() != null ? userAchievement.getClassroom().getId() : null,
      userAchievement.getEarnedAt()
    );
  }
}
