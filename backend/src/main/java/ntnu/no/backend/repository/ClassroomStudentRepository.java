package ntnu.no.backend.repository;

import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.NameChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ClassroomStudent} entities.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomStudent
 */
@Repository
public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudent, ClassroomStudentId> {

  /**
   * Retrieves a students classroom membership by user.
   *
   * @param user the student user
   * @return an {@link Optional} containing the membership, or empty if not found
   */
  Optional<ClassroomStudent> findByUser(User user);

  /**
   * Checks whether a student is already in a classroom.
   *
   * @param user the student user
   * @return {@code true} if the student has a classroom membership
   */
  boolean existsByUser(User user);

  /**
   * Retrieves all student memberships for a classroom.
   *
   * @param classroom the classroom to list students for
   * @return a list of student memberships
   */
  List<ClassroomStudent> findAllByClassroom(Classroom classroom);

  /**
   * Retrieves student memberships for a classroom filtered by approved students.
   *
   * @param classroom the classroom to list students for
   * @return a list of approved student memberships
   */
  List<ClassroomStudent> findAllByClassroomAndAllowedIsTrue(Classroom classroom);

  /**
   * Retrieves student memberships for a classroom filtered by not approved (pending)
   *
   * @param classroom the classroom to list students for
   * @return a list of pending student memberships
   */
  List<ClassroomStudent> findAllByClassroomAndAllowedIsFalse(Classroom classroom);

  /**
   * Retrieves a specific students membership in a classroom.
   *
   * @param classroom the classroom
   * @param user the student user
   * @return an {@link Optional} containing the membership, or empty if not found
   */
  Optional<ClassroomStudent> findByClassroomAndUser(Classroom classroom, User user);

  /**
   * Retrieves student memberships for a classroom filtered by name change status.
   *
   * @param classroom the classroom
   * @param nameChangeStatus the status to filter by
   * @return a list of matching student memberships
   */
  List<ClassroomStudent> findAllByClassroomAndNameChangeStatus(Classroom classroom, NameChangeStatus nameChangeStatus);
}
