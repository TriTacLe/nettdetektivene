package ntnu.no.backend.service;

import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.TaskBestScore;
import ntnu.no.backend.model.TaskBestScoreId;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LeaderboardService}.
 * Verifies ranking logic and filtering without hitting the database.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

  @Mock
  TaskBestScoreRepository taskBestScoreRepository;

  @InjectMocks
  LeaderboardService leaderboardService;

  /**
   * Builds a {@link TaskBestScore} with the given user and score for use in tests.
   */
  private TaskBestScore buildScore(User user, Task task, Classroom classroom, int bestScore) {
    return TaskBestScore.builder()
        .id(new TaskBestScoreId(user.getId(), task.getId(), classroom.getId()))
        .user(user)
        .task(task)
        .classroom(classroom)
        .bestScore(bestScore)
        .totalAttempts(1)
        .lastAttemptedAt(LocalDateTime.now())
        .build();
  }

  /**
   * Verifies that students are ranked correctly by total score in descending order.
   */
  @Test
  void getClassroomLeaderboard_returnsRankedList() {
    UUID classroomId = UUID.randomUUID();

    User user1 = User.builder().id(UUID.randomUUID()).displayName("Nora").build();
    User user2 = User.builder().id(UUID.randomUUID()).displayName("Tri").build();
    Task task = Task.builder().id(UUID.randomUUID()).title("Spot the phishing email").build();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();

    TaskBestScore score1 = buildScore(user1, task, classroom, 50);
    TaskBestScore score2 = buildScore(user2, task, classroom, 80);

    when(taskBestScoreRepository.findByClassroomId(classroomId))
        .thenReturn(List.of(score1, score2));

    List<LeaderboardEntryResponseDTO> result = leaderboardService.getClassroomLeaderboard(classroomId);

    assertThat(result).hasSize(2);
    assertThat(result.getFirst().displayName()).isEqualTo("Tri");
    assertThat(result.get(0).totalScore()).isEqualTo(80);
    assertThat(result.get(0).rank()).isEqualTo(1);
    assertThat(result.get(1).displayName()).isEqualTo("Nora");
    assertThat(result.get(1).rank()).isEqualTo(2);
  }

  /**
   * Verifies that an empty list is returned when no scores exist for the classroom.
   */
  @Test
  void getClassroomLeaderboard_returnsEmpty_whenNoScores() {
    UUID classroomId = UUID.randomUUID();
    when(taskBestScoreRepository.findByClassroomId(classroomId)).thenReturn(List.of());

    List<LeaderboardEntryResponseDTO> result = leaderboardService.getClassroomLeaderboard(classroomId);

    assertThat(result).isEmpty();
  }

  /**
   * Verifies that only the target student's scores are returned when filtering by userId.
   */
  @Test
  void getStudentScore_returnsScores_whenFound() {
    UUID classroomId = UUID.randomUUID();

    User user1 = User.builder().id(UUID.randomUUID()).displayName("Nora").build();
    User user2 = User.builder().id(UUID.randomUUID()).displayName("Tri").build();
    Task task = Task.builder().id(UUID.randomUUID()).title("Spot the phishing email").build();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();

    TaskBestScore score1 = buildScore(user1, task, classroom, 50);
    TaskBestScore score2 = buildScore(user2, task, classroom, 80);

    when(taskBestScoreRepository.findByClassroomId(classroomId))
        .thenReturn(List.of(score1, score2));

    List<TaskBestScoreResponseDTO> result = leaderboardService.getStudentScore(classroomId, user1.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo(user1.getId());
    assertThat(result.get(0).bestScore()).isEqualTo(50);
  }

  /**
   * Verifies that an empty list is returned when the student has no scores in the classroom.
   */
  @Test
  void getStudentScore_returnsEmpty_whenStudentHasNoScores() {
    UUID classroomId = UUID.randomUUID();
    UUID unknownUserId = UUID.randomUUID();

    User user = User.builder().id(UUID.randomUUID()).displayName("Nora").build();
    Task task = Task.builder().id(UUID.randomUUID()).title("Spot the phishing email").build();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();

    when(taskBestScoreRepository.findByClassroomId(classroomId))
        .thenReturn(List.of(buildScore(user, task, classroom, 50)));

    List<TaskBestScoreResponseDTO> result = leaderboardService.getStudentScore(classroomId, unknownUserId);

    assertThat(result).isEmpty();
  }
}
