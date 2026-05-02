package ntnu.no.backend.dto.institution;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing an institution.
 *
 * @param id        unique identifier of the institution
 * @param instName  name of the institution
 * @param createdAt timestamp of when the institution was created
 * @param updatedAt timestamp of the last update
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents an institution")
public record InstitutionResponseDTO(
    UUID id,
    String instName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
