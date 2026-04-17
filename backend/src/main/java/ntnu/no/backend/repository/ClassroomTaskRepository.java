package ntnu.no.backend.repository;

import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.ClassroomTaskId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see ClassroomTask
 * @see ClassroomTaskId
 */
@Repository
public interface ClassroomTaskRepository extends JpaRepository<ClassroomTask, ClassroomTaskId> {
}
