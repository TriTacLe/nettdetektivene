package ntnu.no.backend.dto.sidequest;

import io.swagger.v3.oas.annotations.media.Schema;
import ntnu.no.backend.model.SideQuestStatus;

@Schema(description = "Per-user state for one side quest")
public record SideQuestStateDTO(
    String key,
    int stage,
    SideQuestStatus status
) {}
