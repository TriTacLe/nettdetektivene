package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO returned after successful authentication.
 *
 * @param token short-lived JWT access token used to authenticate requests
 * @param refreshToken long-lived refresh token used to obtain a new access token when it expires
 * @param roles the roles assigned to the authenticated user
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Response body containing a JWT access token and a refresh token after successful authentication")
public record AuthResponseDTO(
    String token,
    String refreshToken,
    List<String> roles
) {}
