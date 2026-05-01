package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.mapper.UserAchievementMapper;
import ntnu.no.backend.model.Achievement;
import ntnu.no.backend.model.UserAchievement;
import ntnu.no.backend.model.UserAchievementId;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.AchievementRepository;
import ntnu.no.backend.repository.UserAchievementRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service for reading achievements earned by users, plus awarding the
 * narrow set of achievements that originate from client events rather
 * than DB triggers (currently: {@code secret} achievements).
 *
 * <p>Most achievements are still awarded automatically by database
 * triggers ({@code trg_attempt_sync}, {@code trg_notify_stop_completed},
 * {@code trg_mystery_approved}); do NOT add general-purpose award logic
 * here.</p>
 *
 * @author tri, norakirknes
 * @version 0.3
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class UserAchievementService {
  private final UserAchievementRepository userAchievementRepository;
  private final AchievementRepository achievementRepository;
  private final UserRepository userRepository;

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

  /**
   * Awards a {@code secret} achievement (looked up by its trigger ref)
   * to the given user. Idempotent: if the user already owns it, the
   * existing grant is returned untouched.
   *
   * @param userId      the user to award
   * @param triggerRef  the {@code trigger_ref} of the secret achievement
   * @return the resulting {@link UserAchievementResponseDTO}
   */
  @Transactional
  public UserAchievementResponseDTO awardSecret(UUID userId, String triggerRef) {
    Achievement achievement = achievementRepository
        .findByTriggerTypeAndTriggerRef("secret", triggerRef)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown secret achievement"));

    UserAchievementId id = new UserAchievementId(userId, achievement.getId());
    return userAchievementRepository.findById(id)
        .map(UserAchievementMapper::toDTO)
        .orElseGet(() -> {
          User user = userRepository.findById(userId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
          UserAchievement saved = userAchievementRepository.save(UserAchievement.builder()
              .id(id)
              .user(user)
              .achievement(achievement)
              .build());
          return UserAchievementMapper.toDTO(saved);
        });
  }
}
