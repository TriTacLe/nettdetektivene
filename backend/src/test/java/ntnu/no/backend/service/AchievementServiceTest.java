package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.achievement.AchievementResponseDTO;
import ntnu.no.backend.model.Achievement;
import ntnu.no.backend.repository.AchievementRepository;
import ntnu.no.backend.util.AchievementTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AchievementService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

  @Mock
  private AchievementRepository achievementRepository;

  @InjectMocks
  private AchievementService achievementService;

  @Test
  void getAllAchievement_returnsMappedDTOs() {
    when(achievementRepository.findAll()).thenReturn(List.of(AchievementTestFactory.achievement()));

    List<AchievementResponseDTO> result = achievementService.getAllAchievement();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("First Mystery");
  }

  @Test
  void getAchievement_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(achievementRepository.findById(id)).thenReturn(Optional.of(AchievementTestFactory.achievement()));

    AchievementResponseDTO result = achievementService.getAchievement(id);

    assertThat(result.title()).isEqualTo("First Mystery");
  }

  @Test
  void getAchievement_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(achievementRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> achievementService.getAchievement(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void getAchievementsByTrigger_returnsMappedDTOs() {
    when(achievementRepository.findByTriggerType("ukens_mysterium"))
      .thenReturn(List.of(AchievementTestFactory.achievement()));

    List<AchievementResponseDTO> result = achievementService.getAchievementsByTrigger("ukens_mysterium");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).triggerType()).isEqualTo("ukens_mysterium");
  }
}
