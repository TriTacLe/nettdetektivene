package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Data Transfer Object for simulated FEIDE login.
 * Contains the display name, email and affiliation returned by FEIDE.
 * The {@code affiliation} field simulates the {@code eduPersonAffiliation} claim
 * that real FEIDE would assert, {@code student} or {@code instructor}.
 * If no account exists for the given email, a new user and profile are created automatically.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body for simulated FEIDE login")
public class FeideLoginRequestDTO {

  /**
   * The display name returned by FEIDE.
   */
  @NotBlank(message = "Display name is required")
  @Schema(description = "Display name returned by FEIDE", example = "Firstname Lastname")
  private String displayName;

  /**
   * The email address returned by FEIDE.
   */
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Schema(description = "Email address returned by FEIDE", example = "firstname.lastname@skole.oslo.kommune.no")
  private String email;

  /**
   * Simulates the FEIDE {@code eduPersonAffiliation} claim.
   * Accepted values: {@code student} or {@code instructor}.
   * Used on first login to determine role assignment, which is the
   * "source of truth" for all later requests.
   */
  @NotBlank(message = "Affiliation is required")
  @Schema(description = "Simulated eduPersonAffiliation claim from FEIDE", allowableValues = {"student", "instructor"}, example = "student")
  private String affiliation;
}
