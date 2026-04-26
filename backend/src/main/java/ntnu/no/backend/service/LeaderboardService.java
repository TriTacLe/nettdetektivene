package ntnu.no.backend.service;


import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.ClassroomLeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.mapper.TaskBestScoreMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.TaskBestScore;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toMap;

/**
 * Service for reading leaderboard data within a classroom.
 * All data is read-only. {@code task_best_score} is maintained exclusively
 * by the {@code trg_attempt_sync} database trigger.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {
  private final TaskBestScoreRepository taskBestScoreRepository;
  private final ClassroomRepository classroomRepository;
  private final UserProfileRepository userProfileRepository;

  /**
   * Returns all students in a classroom ranked by their total score,
   * aggregated across all tasks. Rank 1 (rangering) is the highest score.
   *
   * @param classroomId the classroom to fetch the leaderboard for
   * @return ranked list of {@link LeaderboardEntryResponseDTO}, sorted descending by total score
   */
  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponseDTO> getClassroomLeaderboard(UUID classroomId){
    List<TaskBestScore> scores = taskBestScoreRepository.findByClassroomId(classroomId);

    // group by user, sum their best score across all tasks
    Map<User, Integer> totalByUser = scores.stream().collect(Collectors.groupingBy(TaskBestScore::getUser, Collectors.summingInt(TaskBestScore::getBestScore)));

    // fetch xpLevel for all users in one query
    List<User> users = new ArrayList<>(totalByUser.keySet());
    record ProfileData(int xpLevel, int xp) {}
    Map<UUID, ProfileData> profileByUserId = userProfileRepository.findByUserIn(users).stream()
        .collect(Collectors.toMap(p -> p.getUser().getId(), p -> new ProfileData(p.getXpLevel(), p.getXp())));

    // sort desc
    List<Map.Entry<User, Integer>> sorted = totalByUser.entrySet().stream().sorted(Map.Entry.<User, Integer>comparingByValue().reversed()).toList();

    // assign rank
    List<LeaderboardEntryResponseDTO> result = new ArrayList<>();
    for (int i=0; i<sorted.size();i++){
      User user = sorted.get(i).getKey();
      var profile = profileByUserId.getOrDefault(user.getId(), new ProfileData(1, 0));
      result.add(new LeaderboardEntryResponseDTO(user.getId(), user.getDisplayName(), sorted.get(i).getValue(), i+1, profile.xpLevel(), profile.xp()));
    }

    return result;
  }

  /**
   * Returns all classrooms ranked by their total score across all students.
   *
   * @return ranked list of {@link ClassroomLeaderboardEntryResponseDTO}, sorted descending by total score
   */
  @Transactional(readOnly = true)
  public List<ClassroomLeaderboardEntryResponseDTO> getClassroomsLeaderboard(UUID institutionId){
    Map<UUID, Integer> scoreByClassroom = taskBestScoreRepository.findAll().stream().filter(tbs -> tbs.getClassroom().getInstitution().getId().equals(institutionId))
        .collect(groupingBy(tbs -> tbs.getClassroom().getId(), summingInt(TaskBestScore::getBestScore)));

    Map<UUID, String> titlesByClassroom = classroomRepository.findAllByDeletedAtIsNull().stream()
        .collect(toMap(Classroom::getId, Classroom::getTitle));

    var ranked = scoreByClassroom.entrySet().stream()
        .filter(e -> titlesByClassroom.containsKey(e.getKey()))
        .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
        .toList();

    var result = new ArrayList<ClassroomLeaderboardEntryResponseDTO>();
    for (int i = 0; i < ranked.size(); i++) {
      var entry = ranked.get(i);
      result.add(new ClassroomLeaderboardEntryResponseDTO(entry.getKey(), titlesByClassroom.get(entry.getKey()), entry.getValue(), i + 1));
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

  @Transactional(readOnly = true)
  public long countPassedTasksByStopLocation(UUID classroomId, UUID userId, String stopLocation) {
    return taskBestScoreRepository.countByClassroomIdAndUserIdAndTaskStopLocationAndFirstPassedAtIsNotNull(
        classroomId,
        userId,
        stopLocation
    );
  }
}
