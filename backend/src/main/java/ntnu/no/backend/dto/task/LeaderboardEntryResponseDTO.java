package ntnu.no.backend.dto.task;

import java.util.UUID;

/**
 * Response DTO representing a student's aggregated position on the classroom leaderboard.
 *
 * @param userId      unique identifier of the student
 * @param displayName the student's display name
 * @param totalScore  sum of best scores across all tasks in the classroom
 * @param rank        leaderboard position, starting at 1 (highest score)
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public record LeaderboardEntryResponseDTO(UUID userId, String displayName, int totalScore, int rank){ }
