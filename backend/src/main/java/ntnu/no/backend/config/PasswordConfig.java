package ntnu.no.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding.
 * Provides a BCrypt encoder for secure password hashing.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Configuration
public class PasswordConfig {

  /**
   * BCrypt password encoder for hashing user passwords.
   *
   * @return the password encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
