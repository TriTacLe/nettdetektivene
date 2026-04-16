package ntnu.no.backend.model;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ntnu.no.backend.model.auth.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a notification sent to a user, optionally linked to a classroom.
 * Notifications are created automatically by the {@code trg_mystery_approved} trigger
 * — do NOT create them manually in service code.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name = "notification")
@AttributeOverride(name = "id", column = @Column(name = "notification_id"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity{

  /**
   * The user who receives this notification.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id")
  private User user;

  /**
   * The classroom this notification is related to. May be {@code null}
   * for system-wide notifications.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  /**
   * The notification message shown to the user.
   */
  @Column(nullable = false)
  private String msg;

  /**
   * Optional type of the referenced entity, for example {@code mystery_submission}.
   */
  @Column(name="ref_type")
  private String refType;

  /**
   * Optional ID of the referenced entity.
   */
  @Column(name = "ref_id")
  private UUID refId;

  /**
   * Whether the user has read this notification. Defaults to {@code false}.
   */
  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  /**
   * Timestamp of when this notification was created. Set once on persist.
   */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;
}
