package ntnu.no.backend.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a task that can be assigned to a classroom.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name = "task")
@AttributeOverride(name = "id", column = @Column(name = "task_id", updatable = false, nullable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Task extends BaseEntity {

  @Column(nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false)
  private TaskType taskType;

  @Column(name = "stop_location")
  private String stopLocation;

  @Column(nullable = false)
  private int difficulty = 1;

  @Column(nullable = false, columnDefinition = "JSON")
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
