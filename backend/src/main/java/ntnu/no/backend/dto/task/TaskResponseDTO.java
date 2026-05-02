package ntnu.no.backend.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import ntnu.no.backend.model.enums.TaskType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task.
 *
 * @param id unique identifier of the task
 * @param title title of the task
 * @param taskType category of the task
 * @param stopLocation stop location identifier
 * @param difficulty difficulty level from 1 to 5
 * @param content JSON content of the task
 * @param isActive whether the task is active
 * @param createdAt timestamp of when the task was created
 * @param updatedAt timestamp of the last update
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a task that can be assigned to a classroom")
public record TaskResponseDTO(
    UUID id,
    String title,
    TaskType taskType,
    String stopLocation,
    int difficulty,
    String content,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
