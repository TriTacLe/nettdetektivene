package ntnu.no.backend.service;


import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.mapper.TaskBestScoreMapper;
import ntnu.no.backend.model.TaskBestScore;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for reading leaderboard data within a classroom.
 * All data is read-only. {@code task_best_score} is maintained exclusively
 * by the {@code trg_attempt_sync} database trigger.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {
  private final TaskBestScoreRepository taskBestScoreRepository;

  /**
   * Returns all students in a classroom ranked by their total score,
   * aggregated across all tasks. Rank 1 is the highest score.
   *
   * @param classroomId the classroom to fetch the leaderboard for
   * @return ranked list of {@link LeaderboardEntryResponseDTO}, sorted descending by total score
   */
  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponseDTO> getClassroomLeaderboard(UUID classroomId){
    List<TaskBestScore> scores = taskBestScoreRepository.findByClassroomId(classroomId);

    // group by user, sum their best score across all tasks
    Map<User, Integer> totalByUser = scores.stream().collect(Collectors.groupingBy(TaskBestScore::getUser, Collectors.summingInt(TaskBestScore::getBestScore)));

    // sort desc  
    List<Map.Entry<User, Integer>> sorted = totalByUser.entrySet().stream().sorted(Map.Entry.<User, Integer>comparingByValue().reversed()).toList();

    // assign rank
    List<LeaderboardEntryResponseDTO> result = new ArrayList<>();
    for (int i=0; i<sorted.size();i++){
      User user = sorted.get(i).getKey();
      result.add(new LeaderboardEntryResponseDTO(user.getId(),user.getDisplayName(), sorted.get(i).getValue(),i+1));
    }

    return result;
  }

  /**
   * Returns a specific student's task-by-task best scores within a classroom.
   *
   * @param classroomId the classroom to look up scores in
   * @param userId the student whose scores to retrieve
   * @return list of {@link TaskBestScoreResponseDTO} for the student, may be empty
   */
  public List<TaskBestScoreResponseDTO> getStudentScore(UUID classroomId, UUID userId){
    return taskBestScoreRepository.findByClassroomId(classroomId).stream().filter(s->s.getUser().getId().equals(userId)).map(TaskBestScoreMapper::toDTO).toList();
  }
}
