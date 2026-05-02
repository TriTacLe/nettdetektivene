package ntnu.no.backend.repository;

import ntnu.no.backend.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Achievement} entities.
 *
 * @author Tri
 * @version 0.1
 * @since 0.1
 * @see Achievement
 */
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

  /**
   * Retrieves all achievements associated with a specific trigger type.
   *
   * @param triggerType the trigger type to search by, for example {@code ukens_mysterium}
   * @return a list of achievements with the given trigger type
   */
  List<Achievement> findByTriggerType(String triggerType);
  boolean existsByTitle(String title);

  Optional<Achievement> findByTriggerTypeAndTriggerRef(String triggerType, String triggerRef);
}
