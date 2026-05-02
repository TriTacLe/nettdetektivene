package ntnu.no.backend.repository;

import ntnu.no.backend.model.UserTaskUnlocked;
import ntnu.no.backend.model.UserTaskUnlockedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTaskUnlockedRepository extends JpaRepository<UserTaskUnlocked, UserTaskUnlockedId> {

  List<UserTaskUnlocked> findByUser_IdAndClassroom_Id(UUID userId, UUID classroomId);

  boolean existsByUser_IdAndClassroom_IdAndTask_Id(UUID userId, UUID classroomId, UUID taskId);
}