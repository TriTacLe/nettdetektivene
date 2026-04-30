package ntnu.no.backend.dto.sidequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ntnu.no.backend.model.SideQuestStatus;

/**
 * Update payload for a side quest's state. {@code status} is optional;
 * when omitted the existing status is preserved (typically {@code ACTIVE}).
 */
@Schema(description = "Advance / finalize a side quest's state for the calling user")
public record SideQuestUpdateRequestDTO(
    @NotNull @Min(0) Integer stage,
    SideQuestStatus status
) {}
