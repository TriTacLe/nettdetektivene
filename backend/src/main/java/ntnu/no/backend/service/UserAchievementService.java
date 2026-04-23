package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.mapper.UserAchievementMapper;
import ntnu.no.backend.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for reading achievements earned by users.
 * Achievements are awarded exclusively by the {@code trg_mystery_approved} trigger.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class UserAchievementService {
  private final UserAchievementRepository userAchievementRepository;

  /**
   * Returns all achievements earned by the given user.
   *
   * @param userId the ID of the user
   * @return list of {@link UserAchievementResponseDTO}, may be empty
   */
  @Transactional(readOnly = true)
  public List<UserAchievementResponseDTO> getAchivementsForUser(UUID userId){
    return userAchievementRepository.findByUserId(userId).stream().map(UserAchievementMapper::toDTO).toList();
  }
}
