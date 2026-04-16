package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;

/**
 * Utility class for mapping between {@link User} entities and user-related DTOs.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 * @see UserResponseDTO
 * @see RegisterRequestDTO
 * @see UserUpdateDTO
 */
public class UserMapper {

  private UserMapper() {}

  /**
   * Converts a {@link User} entity to a {@link UserResponseDTO}.
   * Excludes sensitive fields such as the password hash.
   *
   * @param user the user entity to convert
   * @return a {@link UserResponseDTO}, or {@code null} if {@code user} is null
   */
  public static UserResponseDTO toDTO(User user) {
    if (user == null) return null;
    return UserResponseDTO.builder()
      .id(user.getId())
      .displayName(user.getDisplayName())
      .email(user.getEmail())
      .isActive(user.isActive())
      .roles(user.getRoles().stream()
        .map(Role::getAuthority)
        .toList())
      .createdAt(user.getCreatedAt())
      .build();
  }

  /**
   * Converts a {@link RegisterRequestDTO} to a {@link User} entity.
   * Does not set the password — that is handled separately in the service layer.
   *
   * @param dto the registration request to convert
   * @return a {@link User} entity, or {@code null} if {@code dto} is null
   */
  public static User toEntity(RegisterRequestDTO dto) {
    if (dto == null) return null;
    return User.builder()
      .displayName(dto.getDisplayName())
      .email(dto.getEmail())
      .build();
  }

  /**
   * Updates the fields of a {@link User} entity from a {@link UserUpdateDTO}.
   *
   * @param user the user entity to update
   * @param dto  the DTO containing the new values
   */
  public static void updateEntity(User user, UserUpdateDTO dto) {
    if (user == null || dto == null) return;
    user.setDisplayName(dto.getDisplayName());
  }
}
