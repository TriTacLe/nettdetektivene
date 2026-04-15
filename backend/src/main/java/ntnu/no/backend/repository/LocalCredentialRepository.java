package ntnu.no.backend.repository;

import ntnu.no.backend.model.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see LocalCredential
 */
public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {
}
