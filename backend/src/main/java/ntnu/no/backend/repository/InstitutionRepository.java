package ntnu.no.backend.repository;

import ntnu.no.backend.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Institution} entities.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
  Optional<Institution> findByInstName(String instName);
}
