package ntnu.no.backend.util;

import ntnu.no.backend.dto.achievement.AchievementResponseDTO;
import ntnu.no.backend.model.Achievement;

import java.time.LocalDateTime;
import java.util.UUID;

public class AchievementTestFactory {

  public static Achievement achievement() {
    return Achievement.builder()
      .title("First Mystery")
      .descAch("Solve a mystery")
      .icon("icon1")
      .triggerType("ukens_mysterium")
      .build();
  }

  public static AchievementResponseDTO responseDTO(UUID id) {
    return new AchievementResponseDTO(
      id, "First Mystery", "Solve a mystery", "icon1",
      "ukens_mysterium", null, LocalDateTime.now()
    );
  }
}
