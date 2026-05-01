package ntnu.no.backend.exception;

/**
 * Thrown when an operation cannot be completed due to a conflict
 * with the current state of the resource.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
