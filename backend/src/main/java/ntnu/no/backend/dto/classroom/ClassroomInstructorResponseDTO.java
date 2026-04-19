package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing an instructors assignment to a classroom.
 *
 * @param classroomId the classroom ID
 * @param userId the instructors user ID
 * @param assignedAt timestamp of when the instructor was assigned
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents an instructors assignment to a classroom")
public record ClassroomInstructorResponseDTO(
    UUID classroomId,
    UUID userId,
    LocalDateTime assignedAt
) {}
