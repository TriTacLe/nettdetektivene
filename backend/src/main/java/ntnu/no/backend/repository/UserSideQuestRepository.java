package ntnu.no.backend.repository;

import ntnu.no.backend.model.UserSideQuest;
import ntnu.no.backend.model.UserSideQuestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSideQuestRepository extends JpaRepository<UserSideQuest, UserSideQuestId> {
  List<UserSideQuest> findByIdUserId(UUID userId);
}
