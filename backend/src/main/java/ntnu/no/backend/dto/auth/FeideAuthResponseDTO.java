package ntnu.no.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned after successful FEIDE authentication simulation.
 * Extends the auth response with a FEIDE identifier embedded in the JWT.
 * The client does not need to store or resend it, as "later login" resolution
 * is handled server-side via the JWT claim.
 *
 * @param token short-lived JWT access token used to authenticate requests, contains feideId as a claim
 * @param refreshToken long-lived refresh token used to obtain a new access token when it expires
 * @param role the role of the authenticated user
 * @param feideId the generated FEIDE identifier for this user, mirroring the {@code sub} claim in a real FEIDE token
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Response body returned after successful FEIDE authentication")
public record FeideAuthResponseDTO(
    String token,
    String refreshToken,
    String role,
    String feideId
) {}
