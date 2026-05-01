package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding an instructor to a classroom.
 *
 * @param email the email address of the teacher to add
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Request to add an instructor to a classroom")
public record AddInstructorRequestDTO(
    @NotBlank @Email String email
) {}
