package ntnu.no.backend.repository;

import ntnu.no.backend.model.NotebookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link NotebookEntry} entities.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public interface NotebookEntryRepository extends JpaRepository<NotebookEntry, UUID> {

  List<NotebookEntry> findByClassroom_IdAndUser_Id(UUID classroomId, UUID userID);


  List<NotebookEntry> findByClassroom_Id(UUID classroomId);
}
