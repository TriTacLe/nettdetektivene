package ntnu.no.backend.repository;

import ntnu.no.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see Notification
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserId(UUID userId);
}
