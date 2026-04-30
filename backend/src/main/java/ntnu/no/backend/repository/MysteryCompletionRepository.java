package ntnu.no.backend.repository;

import ntnu.no.backend.model.MysteryCompletion;
import ntnu.no.backend.model.MysteryCompletionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link MysteryCompletion} entities.
 */
@Repository
public interface MysteryCompletionRepository extends JpaRepository<MysteryCompletion, MysteryCompletionId> {

  boolean existsByIdUserIdAndIdSubmissionId(UUID userId, UUID submissionId);

  List<MysteryCompletion> findByIdUserId(UUID userId);

  long countByIdUserId(UUID userId);
}
