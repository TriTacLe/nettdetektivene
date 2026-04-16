package ntnu.no.backend.shared;

import lombok.AllArgsConstructor;

/**
 * Generic response wrapper ensuring all endpoints return a consistent JSON structure.
 * Use {@link #ok(Object)} for success responses and {@link #error(String)} for error responses.
 *
 * @param <T> the type of the response payload
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public record ApiResponse<T>(boolean success, String message, T data) {

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, message, data);
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, null, data);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, message, null);
  }
}
