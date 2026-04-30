package ntnu.no.backend.repository;

import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.enums.MysteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link MysterySubmission} entities.
 */
@Repository
public interface MysterySubmissionRepository extends JpaRepository<MysterySubmission, UUID> {

  List<MysterySubmission> findByClassroom_Id(UUID classroomId);

  List<MysterySubmission> findByClassroom_IdAndUserSubmittedBy_Id(UUID classroomId, UUID userId);

  List<MysterySubmission> findByClassroom_IdAndMysStatus(UUID classroomId, MysteryStatus mysStatus);

  /** Approved submissions in a classroom, excluding ones the given user submitted. */
  List<MysterySubmission> findByClassroom_IdAndMysStatusAndUserSubmittedBy_IdNot(
      UUID classroomId, MysteryStatus mysStatus, UUID excludedUserId);
}
