package ntnu.no.backend.repository;

import ntnu.no.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link Task} entities.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {
}
