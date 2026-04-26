package ntnu.no.backend.util;

import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public class LeaderboardTestFactory {
  public static LeaderboardEntryResponseDTO entryResponseDTO(UUID userID){
    return new LeaderboardEntryResponseDTO(userID, "Tri", 100, 1, 1, 0);
  }

  public static TaskBestScoreResponseDTO scoreResponseDTO(UUID userId, UUID taskId, UUID classroomId){
    return new TaskBestScoreResponseDTO(userId, taskId, classroomId, 80, 3, null, LocalDateTime.now());
  }
}
