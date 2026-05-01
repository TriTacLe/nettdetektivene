package ntnu.no.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.shared.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler mapping service exceptions to HTTP responses.
 *
 * @author norakirknes, tri
 * @version 0.1
 * @since 0.1
 * @see EmailAlreadyExistsException
 * @see EntityNotFoundException
 * @see BadCredentialsException
 * @see DisabledException
 * @see LockedException
 * @see RefreshTokenException
 * @see MethodArgumentNotValidException
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles registration attempt with an already registered email
   *
   * @param e the exception thrown when email already exists
   * @return 409 Conflict with generic error message
   */
  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Email already in use"));
  }

  /**
   * Handles not found entity lookups
   *
   * @param e the exception thrown when entity is not found
   * @return a 404 Not Found with error message
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
  }

  /**
   * Handles bad credentials; failed authentication attempt
   *
   * @param e the exception thrown for invalid credentials
   * @return 401 Unauthorized with generic message
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
    logger.warn("Failed login attempt");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid email or password"));
  }

  /**
   * Handles disabled account login attempts
   *
   * @param e the exception thrown when an account is disabled
   * @return 403 Forbidden
   */
  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Account is disabled"));
  }

  /**
   * Handles locked account login attempts
   *
   * @param e the exception thrown when the account is locked
   * @return 403 Forbidden
   */
  @ExceptionHandler(LockedException.class)
  public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Account is locked"));
  }

  /**
   * Handles an invalid refresh token; expired, revoked etc.
   *
   * @param e the exception thrown when refresh token is invalid
   * @return 401 Unauthorized with generic error message
   */
  @ExceptionHandler(RefreshTokenException.class)
  public ResponseEntity<ApiResponse<Void>> handleRefreshToken(RefreshTokenException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid refresh token"));
  }

  /**
   * Handles illegal argument errors such as an unrecognized affiliation claim
   *
   * @param e the exception thrown for invalid argument values
   * @return 400 Bad Request with the error message
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
  }

  /**
   * Handles validation failures from {@code @Valid} annotations
   *
   * @param e the exception containing field validation errors
   * @return 400 Bad Request with separated field errors
   */
  /**
   * Handles conflict errors such as duplicate instructor assignments
   *
   * @param e the exception thrown on a state conflict
   * @return 409 Conflict with error message
   */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
    String errors = e.getBindingResult().getFieldErrors().stream()
      .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
      .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errors));
  }

  /**
   * Catch-all for any unhandled exception. Without this, unhandled exceptions
   * fall through to Spring Boot's error endpoint, which is not in the public
   * endpoint list and gets rejected as 401 by Spring Security.
   *
   * @param e the unhandled exception
   * @return 500 Internal Server Error with generic message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    logger.error("Unhandled exception", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ApiResponse.error("En uventet feil oppsto"));
  }
}
