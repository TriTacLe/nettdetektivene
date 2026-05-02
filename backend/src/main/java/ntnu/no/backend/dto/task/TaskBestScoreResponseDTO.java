package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a student's best score on a task within a classroom.
 * This is read-only — updated exclusively by the {@code trg_attempt_sync} trigger.
 *
 * @param userId unique identifier of the student
 * @param taskId unique identifier of the task
 * @param classroomId unique identifier of the classroom
 * @param bestScore highest score achieved on this task
 * @param totalAttempts total number of attempts made
 * @param firstPassedAt timestamp of when the student first passed this task
 * @param lastAttemptedAt timestamp of the most recent attempt
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a student's best score on a task within a classroom")
public record TaskBestScoreResponseDTO(
    UUID userId,
    UUID taskId,
    UUID classroomId,
    int bestScore,
    int totalAttempts,
    LocalDateTime firstPassedAt,
    LocalDateTime lastAttemptedAt
) {}
