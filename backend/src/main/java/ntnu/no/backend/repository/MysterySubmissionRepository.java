package ntnu.no.backend.repository;

import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.enums.MysteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MysterySubmission} entities.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 * @see MysterySubmission
 */
@Repository
public interface MysterySubmissionRepository extends JpaRepository<MysterySubmission, UUID> {

  /**
   * Finds all submissions in a classroom.
   *
   * @param classroomId the classroom id
   * @return submissions for the classroom
   */
  List<MysterySubmission> findByClassroom_Id(UUID classroomId);

  /**
   * Finds all submissions in a classroom with a given status.
   *
   * @param classroomId the classroom id
   * @param mysStatus the submission status
   * @return matching submissions
   */
  List<MysterySubmission> findByClassroom_IdAndMysStatus(UUID classroomId, MysteryStatus mysStatus);

  /**
   * Finds the featured submission for a classroom.
   *
   * @param classroomId the classroom id
   * @return the featured submission, if one exists
   */
  Optional<MysterySubmission> findFirstByClassroom_IdAndIsFeaturedTrue(UUID classroomId);
}
