package ntnu.no.backend.exception;

public class RefreshTokenException extends RuntimeException {
  public RefreshTokenException(String message) {
    super(message);
  }
}
