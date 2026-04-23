package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.mapper.UserMapper;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for user account management.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;


  @Transactional(readOnly = true)
  public UserResponseDTO getUser(UUID id) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No user found with id: " + id));
    return UserMapper.toDTO(user);
  }


  @Transactional
  public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No user found with id: " + id));
    UserMapper.updateEntity(user, dto);
    userProfileRepository.findByUser(user)
      .ifPresent(profile -> profile.setDisplayName(user.getDisplayName()));
    return UserMapper.toDTO(user);
  }
}
