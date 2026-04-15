package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.Role;
import ntnu.no.backend.model.User;

/**
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


  public static User toEntity(RegisterRequestDTO dto) {
    if (dto == null) return null;
    return User.builder()
      .displayName(dto.getDisplayName())
      .email(dto.getEmail())
      .build();
  }

  public static void updateEntity(User user, UserUpdateDTO dto) {
    if (user == null || dto == null) return;
    user.setDisplayName(dto.getDisplayName());
  }
}
