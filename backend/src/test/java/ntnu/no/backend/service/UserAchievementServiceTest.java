package ntnu.no.backend.service;

import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.model.Achievement;
import ntnu.no.backend.model.UserAchievement;
import ntnu.no.backend.model.UserAchievementId;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.UserAchievementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserAchievementService}.
 * Verifies that achievements are correctly fetched and mapped for a given user.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
public class UserAchievementServiceTest {
  @Mock
  UserAchievementRepository userAchievementRepository;

  @InjectMocks
  UserAchievementService userAchievementService;

  /**
   * Verifies that a list of achievements is returned and correctly mapped when the user has earned achievements.
   */
  @Test
  void getAchivementsForUser_returnsList_whenFound() {
    UUID userId = UUID.randomUUID();
    UUID achievementId = UUID.randomUUID();

    User user = User.builder().id(userId).displayName("Tri").build();
    Achievement achievement = Achievement.builder().id(achievementId).build();
    UserAchievement ua = UserAchievement.builder()
            .id(new UserAchievementId(userId, achievementId))
            .user(user)
            .achievement(achievement)
            .classroom(null)
            .earnedAt(LocalDateTime.now())
            .build();

    when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));

    List<UserAchievementResponseDTO> result = userAchievementService.getAchievementsForUser(userId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().userId()).isEqualTo(userId);
    assertThat(result.getFirst().achievementId()).isEqualTo(achievementId);
    assertThat(result.getFirst().classroomId()).isNull();
  }

  /**
   * Verifies that an empty list is returned when the user has no achievements.
   */
  @Test
  void getAchivementsForUser_returnsEmpty_whenNoneFound() {
    UUID userId = UUID.randomUUID();
    when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of());

    List<UserAchievementResponseDTO> result = userAchievementService.getAchievementsForUser(userId);

    assertThat(result).isEmpty();
  }
}
