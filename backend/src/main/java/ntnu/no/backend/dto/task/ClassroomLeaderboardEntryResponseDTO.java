package ntnu.no.backend.dto.task;

import java.util.UUID;

/**
 * Response DTO representing a classroom's aggregated position on the global leaderboard.
 *
 * @param classroomId unique identifier of the classroom
 * @param title       the classroom title
 * @param totalScore  sum of all students' best scores across all tasks in the classroom
 * @param rank        leaderboard position, starting at 1 (highest score)
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public record ClassroomLeaderboardEntryResponseDTO(UUID classroomId, String title, int totalScore, int rank) {}
