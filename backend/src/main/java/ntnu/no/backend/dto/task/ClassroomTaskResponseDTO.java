package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO representing a task assigned to a classroom.
 *
 * @param classroomId the classroom ID
 * @param taskId the task ID
 * @param sortOrder display order within the classroom
 * @param isActive whether the task is active for this classroom
 * @param task the underlying task
 */
@Schema(description = "Represents a task assignment within a classroom")
public record ClassroomTaskResponseDTO(
    UUID classroomId,
    UUID taskId,
    short sortOrder,
    Boolean isActive,
    boolean isUnlocked,
    TaskResponseDTO task
) {}