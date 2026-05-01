package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ntnu.no.backend.model.auth.User;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Per-user state for one side quest. Backs the side-quest system that
 * gates branching one-shot stories triggered from the game map.
 *
 * @see UserSideQuestId
 * @see SideQuestStatus
 */
@Entity
@Table(name = "user_side_quest")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSideQuest {

  @EmbeddedId
  private UserSideQuestId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "stage", nullable = false)
  private int stage;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SideQuestStatus status;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
