package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 *
 * @author tri
 * @author aninesst
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @InjectMocks
  private UserService userService;

  private User sampleUser() {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("test@example.com");
    return user;
  }

  @Test
  void getUser_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserResponseDTO result = userService.getUser(id);

    assertThat(result.getDisplayName()).isEqualTo("Test User");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void getUser_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(id))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void updateUser_returnsUpdatedDTO_whenFound() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    UserResponseDTO result = userService.updateUser(id, dto);

    assertThat(result.getDisplayName()).isEqualTo("Updated Name");
  }

  @Test
  void updateUser_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    assertThatThrownBy(() -> userService.updateUser(id, dto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void updateUser_partialUpdate_preservesUnchangedFields() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Partially Updated")
        .build();

    UserResponseDTO result = userService.updateUser(id, dto);

    assertThat(result.getDisplayName()).isEqualTo("Partially Updated");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }
}