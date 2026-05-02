package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.AchievementResponseDTO;
import ntnu.no.backend.mapper.AchievementMapper;
import ntnu.no.backend.model.Achievement;
import ntnu.no.backend.repository.AchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing achievements.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;

  /**
   * Returns all achievements.
   *
   * @return list of all achievements as DTOs
   */
  @Transactional(readOnly = true)
  public List<AchievementResponseDTO> getAllAchievement() {
    return achievementRepository.findAll().stream().map(AchievementMapper::toDTO).toList();
  }

  /**
   * Returns a single achievement by ID.
   *
   * @param id the achievement ID
   * @return the achievement as a DTO
   * @throws EntityNotFoundException if no achievement found with the given ID
   */
  @Transactional(readOnly = true)
  public AchievementResponseDTO getAchievement(UUID id) {
    Achievement achievement = achievementRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No achievement found with id: " + id));
    return AchievementMapper.toDTO(achievement);
  }

  /**
   * Returns all achievements matching a given trigger type.
   *
   * @param triggerType the trigger type to filter by, for example {@code ukens_mysterium}
   * @return list of matching achievements as DTOs
   */
  @Transactional(readOnly = true)
  public List<AchievementResponseDTO> getAchievementsByTrigger(String triggerType) {
    return achievementRepository.findByTriggerType(triggerType).stream().map(AchievementMapper::toDTO).toList();
  }
}
