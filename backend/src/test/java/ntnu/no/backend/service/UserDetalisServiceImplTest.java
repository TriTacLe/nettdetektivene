package ntnu.no.backend.service;

import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserDetailsServiceImpl}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserDetailsServiceImpl userDetailsService;

  private User sampleUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .displayName("Test User")
        .email("test@example.com")
        .build();
  }

  private User sampleUserWithRoles() {
    Role sysAdmin = new Role();
    sysAdmin.setRoleName("sys_admin");

    Role instAdmin = new Role();
    instAdmin.setRoleName("inst_admin");

    return User.builder()
        .id(UUID.randomUUID())
        .displayName("Test User")
        .email("test@example.com")
        .roles(List.of(sysAdmin, instAdmin))
        .build();
  }

  @Test
  void loadUserByUsername_returnsUserDetails_whenFound() {
    User user = sampleUser();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("test@example.com");
  }

  @Test
  void loadUserByUsername_throwsUsernameNotFoundException_whenNotFound() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@example.com");
  }

  @Test
  void loadUserByUsername_returnsCorrectRoles() {
    User user = sampleUserWithRoles();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

    assertThat(result.getAuthorities()).hasSize(2);
    assertThat(result.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("sys_admin", "inst_admin");
  }

  @Test
  void loadUserByUsername_returnsCorrectCredentials() {
    User user = sampleUser();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

    assertThat(result).isInstanceOf(User.class);
    User returnedUser = (User) result;
    assertThat(returnedUser.getEmail()).isEqualTo("test@example.com");
    assertThat(returnedUser.getDisplayName()).isEqualTo("Test User");
  }
}