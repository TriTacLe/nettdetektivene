package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.mapper.UserAchievementMapper;
import ntnu.no.backend.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAchievementService {
  private final UserAchievementRepository userAchievementRepository;

  @Transactional(readOnly = true)
  public List<UserAchievementResponseDTO> getAchivementsForUser(UUID userId){
    return userAchievementRepository.findByUserId(userId).stream().map(UserAchievementMapper::toDTO).toList();
  }
}
