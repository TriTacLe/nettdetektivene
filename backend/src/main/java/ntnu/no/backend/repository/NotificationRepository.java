package ntnu.no.backend.repository;

import ntnu.no.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  void deleteByRefTypeAndRefId(String refType, UUID refId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.refType = :refType AND n.refId = :refId AND n.isRead = false")
  void markReadByRefTypeAndRefId(@Param("refType") String refType, @Param("refId") UUID refId);
}
