package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;
import ntnu.no.backend.model.auth.UserProfile;

/**
 * Utility class for mapping between {@link UserProfile} entities and profile-related DTOs.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see UserProfile
 * @see UserProfileResponseDTO
 * @see UserProfileUpdateRequestDTO
 */
public class UserProfileMapper {

  private UserProfileMapper() {}

  /**
   * Converts a {@link UserProfile} entity to a {@link UserProfileResponseDTO}.
   *
   * @param profile the profile entity to convert
   * @return a {@link UserProfileResponseDTO}, or {@code null} if {@code profile} is null
   */
  public static UserProfileResponseDTO toDTO(UserProfile profile) {
    if (profile == null) return null;
    return UserProfileResponseDTO.builder()
      .id(profile.getId())
      .displayName(profile.getDisplayName())
      .feideId(profile.getFeideId())
      .avatarConfig(profile.getAvatarConfig())
      .xp(profile.getXp())
      .xpLevel(profile.getXpLevel())
      .createdAt(profile.getCreatedAt())
      .build();
  }

  /**
   * Updates the fields of a {@link UserProfile} entity from a {@link UserProfileUpdateRequestDTO}.
   * Does not update {@code xp} or {@code xpLevel} — those are managed by database triggers.
   *
   * @param profile the profile entity to update
   * @param dto     the DTO containing the new values
   */
  public static void updateEntity(UserProfile profile, UserProfileUpdateRequestDTO dto) {
    if (profile == null || dto == null) return;
    profile.setDisplayName(dto.getDisplayName());
    profile.setAvatarConfig(dto.getAvatarConfig());
  }
}
