package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.auth.FeideLoginRequestDTO;
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;
import ntnu.no.backend.model.auth.User;
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
 * @see FeideLoginRequestDTO
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
    return new UserProfileResponseDTO(
      profile.getId(),
      profile.getDisplayName(),
      profile.getFeideId(),
      profile.getAvatarConfig(),
      profile.getXp(),
      profile.getXpLevel(),
      profile.getCreatedAt()
    );
  }

  /**
   * Creates a {@link UserProfile} entity from a {@link User} and a {@link FeideLoginRequestDTO}.
   * Used on first FEIDE login to initialize the user's profile.
   * The {@code feideId} is set by the service layer after creation.
   *
   * @param user the user this profile belongs to
   * @param dto the FEIDE login request containing the display name
   * @return a {@link UserProfile} entity, or {@code null} if either argument is null
   */
  public static UserProfile toEntity(User user, FeideLoginRequestDTO dto) {
    if (user == null || dto == null) return null;
    return UserProfile.builder()
      .user(user)
      .displayName(dto.getDisplayName())
      .build();
  }

  /**
   * Updates non-display-name fields of a {@link UserProfile} from a {@link UserProfileUpdateRequestDTO}.
   * Display name updates are handled by the service layer, which routes them through
   * the classroom approval flow when the user is in a classroom.
   * Does not update {@code xp} or {@code xpLevel} (managed by database triggers).
   *
   * @param profile the profile entity to update
   * @param dto the DTO containing the new values
   */
  public static void updateEntity(UserProfile profile, UserProfileUpdateRequestDTO dto) {
    if (profile == null || dto == null) return;
    if (dto.getAvatarConfig() != null) profile.setAvatarConfig(dto.getAvatarConfig());
  }
}
