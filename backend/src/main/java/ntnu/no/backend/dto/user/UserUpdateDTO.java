package ntnu.no.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating a user account.
 * Only fields that are allowed to be changed by the user are included.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request DTO for updating a user account")
public class UserUpdateDTO {

  /**
   * The new display name for the user.
   */
  @NotBlank(message = "Display name is required")
  @Schema(description = "New display name for the user", example = "Firstname Lastname")
  private String displayName;
}
