package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing view of a mystery available for play. Crucially does NOT
 * expose {@code correctIndex} — that would let the client trivially solve
 * any mystery via the Network tab. The correct answer is checked
 * server-side in {@code POST /mysteries/{id}/completion}.
 */
@Schema(description = "Student-facing mystery for play (no correct answer exposed)")
public record MysteryPlayDTO(
    UUID id,
    UUID classroomId,
    String imagePath,
    List<String> alternatives,
    boolean completed
) {}
