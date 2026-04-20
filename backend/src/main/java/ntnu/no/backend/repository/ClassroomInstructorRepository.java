package ntnu.no.backend.repository;

import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ClassroomInstructor} entities.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomInstructor
 */
@Repository
public interface ClassroomInstructorRepository extends JpaRepository<ClassroomInstructor, ClassroomInstructorId> {

  /**
   * Retrieves all instructor assignments for a classroom.
   *
   * @param classroom the classroom to list instructors for
   * @return a list of instructor assignments
   */
  List<ClassroomInstructor> findAllByClassroom(Classroom classroom);

  /**
   * Retrieves all classroom assignments for an instructor.
   *
   * @param user the instructor user
   * @return a list of classroom assignments
   */
  List<ClassroomInstructor> findAllByUser(User user);

  /**
   * Checks whether an instructor is assigned to a classroom.
   *
   * @param classroom the classroom
   * @param user the instructor user
   * @return {@code true} if the instructor is assigned to the classroom
   */
  boolean existsByClassroomAndUser(Classroom classroom, User user);
}
