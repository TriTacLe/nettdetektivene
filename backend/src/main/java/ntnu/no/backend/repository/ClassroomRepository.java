package ntnu.no.backend.repository;

import ntnu.no.backend.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Classroom} entities.
 *
 * @author tri, norakirknes
 * @version 0.1
 * @since 0.1
 */
public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

  /**
   * Returns all classrooms that have not been soft-deleted.
   *
   * @return list of active classrooms
   */
  List<Classroom> findAllByDeletedAtIsNull();

  /**
   * Returns all active classrooms belonging to an institution.
   *
   * @param institutionId the institution ID
   * @return list of active classrooms for the institution
   */
  List<Classroom> findAllByInstitution_IdAndDeletedAtIsNull(UUID institutionId);

  /**
   * Returns an active classroom by ID.
   *
   * @param id the classroom ID
   * @return the classroom if found and not soft-deleted
   */
  Optional<Classroom> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Returns an active classroom by its join code.
   *
   * @param joinCode the classroom join code
   * @return the classroom if found and not soft-deleted
   */
  Optional<Classroom> findByJoinCodeAndDeletedAtIsNull(String joinCode);
}
