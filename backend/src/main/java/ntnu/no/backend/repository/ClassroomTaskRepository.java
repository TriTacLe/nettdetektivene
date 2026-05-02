package ntnu.no.backend.repository;

import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.ClassroomTaskId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 * @see ClassroomTask
 * @see ClassroomTaskId
 */
@Repository
public interface ClassroomTaskRepository extends JpaRepository<ClassroomTask, ClassroomTaskId> {

  List<ClassroomTask> findAllByClassroom_IdOrderBySortOrderAsc(UUID classroomId);

  List<ClassroomTask> findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(UUID classroomId);
}