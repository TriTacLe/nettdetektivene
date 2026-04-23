package ntnu.no.backend.repository;

import ntnu.no.backend.model.UserAchievement;
import ntnu.no.backend.model.UserAchievementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link UserAchievement} entities.
 *
 * @author tri
 * @version 0.2
 * @since 0.1
 * @see UserAchievement
 * @see UserAchievementId
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UserAchievementId> {
  List<UserAchievement> findByUserId(UUID userId);
}