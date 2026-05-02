package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Returned after a student picks an alternative for a mystery.
 *
 * @param correct           whether the picked alternative is the correct one
 * @param completed         whether the mystery is now marked as completed for this user
 * @param alreadyCompleted  whether the mystery had been completed before this pick
 */
@Schema(description = "Result of attempting to solve a mystery")
public record MysteryCompletionResponseDTO(
    boolean correct,
    boolean completed,
    boolean alreadyCompleted
) {}
