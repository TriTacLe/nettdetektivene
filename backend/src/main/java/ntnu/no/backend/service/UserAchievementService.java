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
 * Achievements are awarded exclusively by database triggers
 * ({@code trg_attempt_sync}, {@code trg_notify_stop_completed}, {@code trg_mystery_approved}).
 *
 * @author tri, norakirknes
 * @version 0.2
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
  public List<UserAchievementResponseDTO> getAchievementsForUser(UUID userId){
    return userAchievementRepository.findByUserId(userId).stream().map(UserAchievementMapper::toDTO).toList();
  }
}
