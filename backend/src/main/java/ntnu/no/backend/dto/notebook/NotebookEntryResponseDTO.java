package ntnu.no.backend.dto.notebook;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a notebook entry written by a student.
 *
 * @param id unique identifier of the entry
 * @param userId unique identifier of the student
 * @param classroomId unique identifier of the classroom
 * @param stopLocation stop location this entry belongs to
 * @param autoNotes auto-generated notes in JSON format
 * @param studentReflection student's personal reflection
 * @param createdAt timestamp of when the entry was created
 * @param updatedAt timestamp of the last update
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a notebook entry written by a student")
public record NotebookEntryResponseDTO(
    UUID id,
    UUID userId,
    UUID classroomId,
    String stopLocation,
    String autoNotes,
    String studentReflection,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
