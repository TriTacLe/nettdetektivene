package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.mapper.UserMapper;
import ntnu.no.backend.model.LocalCredential;
import ntnu.no.backend.model.User;
import ntnu.no.backend.repository.LocalCredentialRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.security.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration and login.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JWTUtil jwtUtil;


  @Transactional
  public AuthResponseDTO register(RegisterRequestDTO dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new EmailAlreadyExistsException(dto.getEmail());
    }

    User user = UserMapper.toEntity(dto);
    userRepository.save(user);

    LocalCredential credential = LocalCredential.builder()
      .user(user)
      .passwordHash(passwordEncoder.encode(dto.getPassword()))
      .build();
    localCredentialRepository.save(credential);

    return AuthResponseDTO.builder()
      .token(jwtUtil.generateToken(user))
      .build();
  }


  public AuthResponseDTO login(LoginRequestDTO dto) {
    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
    );

    User user = userRepository.findByEmail(dto.getEmail())
      .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + dto.getEmail()));

    return AuthResponseDTO.builder()
      .token(jwtUtil.generateToken(user))
      .build();
  }
}
