package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a classroom.
 *
 * @param id unique identifier of the classroom
 * @param title title of the classroom
 * @param classDesc description of the classroom
 * @param joinCode code students use to join the classroom
 * @param institutionId ID of the institution this classroom belongs to
 * @param createdById ID of the user who created the classroom
 * @param isGameEnabled whether game mode is enabled
 * @param createdAt timestamp of when the classroom was created
 * @param updatedAt timestamp of the last update
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a classroom")
public record ClassroomResponseDTO(
    UUID id,
    String title,
    String classDesc,
    String joinCode,
    UUID institutionId,
    UUID createdById,
    boolean isGameEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
