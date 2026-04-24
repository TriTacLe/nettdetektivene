package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ntnu.no.backend.model.auth.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_task_unlocked")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserTaskUnlocked {

  @EmbeddedId
  @EqualsAndHashCode.Include
  private UserTaskUnlockedId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("classroomId")
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("taskId")
  @JoinColumn(name = "task_id")
  private Task task;

  @Column(name = "unlocked_at", nullable = false)
  private LocalDateTime unlockedAt;
}