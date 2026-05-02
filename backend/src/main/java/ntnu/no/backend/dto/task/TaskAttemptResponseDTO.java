package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task attempt made by a student.
 *
 * @param id unique identifier of the attempt
 * @param userId unique identifier of the student
 * @param taskId unique identifier of the task
 * @param classroomId unique identifier of the classroom
 * @param score score achieved on this attempt
 * @param passed whether the student passed
 * @param attemptedAt timestamp of when the attempt was made
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a task attempt made by a student")
public record TaskAttemptResponseDTO(
    UUID id,
    UUID userId,
    UUID taskId,
    UUID classroomId,
    int score,
    boolean passed,
    LocalDateTime attemptedAt
) {}
