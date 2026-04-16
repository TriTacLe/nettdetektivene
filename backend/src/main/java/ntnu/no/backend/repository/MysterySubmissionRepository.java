package ntnu.no.backend.repository;

import ntnu.no.backend.model.MysterySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link MysterySubmission} entities.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see MysterySubmission
 */
@Repository
public interface MysterySubmissionRepository extends JpaRepository<MysterySubmission, UUID> {
}
