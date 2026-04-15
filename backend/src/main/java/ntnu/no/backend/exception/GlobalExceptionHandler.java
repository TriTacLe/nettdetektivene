package ntnu.no.backend.exception;

import jakarta.persistence.EntityNotFoundException;
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
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see EmailAlreadyExistsException
 * @see EntityNotFoundException
 * @see BadCredentialsException
 * @see DisabledException
 * @see LockedException
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
  public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
  }

  /**
   * Handles not found entity lookups
   *
   * @param e the exception thrown when entity is not found
   * @return a 404 Not Found with error message
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
    logger.warn(e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }

  /**
   * Handles bad credentials; failed authentication attempt
   *
   * @param e the exception thrown for invalid credentials
   * @return 401 Unauthorized with generic message
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<String> handleBadCredentials(BadCredentialsException e) {
    logger.warn("Failed login attempt");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
  }

  /**
   * Handles disabled account login attempts
   *
   * @param e the exception thrown when an account is disabled
   * @return 403 Forbidden
   */
  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<String> handleDisabled(DisabledException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is disabled");
  }

  /**
   * Handles locked account login attempts
   *
   * @param e the exception thrown when the account is locked
   * @return 403 Forbidden
   */
  @ExceptionHandler(LockedException.class)
  public ResponseEntity<String> handleLocked(LockedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is locked");
  }

  /**
   * Handles validation failures from {@code @Valid} annotations
   *
   * @param e the exception containing field validation errors
   * @return 400 Bad Request with separated field errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> handleValidation(MethodArgumentNotValidException e) {
    String errors = e.getBindingResult().getFieldErrors().stream()
      .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
      .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }
}
