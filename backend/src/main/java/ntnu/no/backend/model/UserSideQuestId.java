package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link UserSideQuest}: the owning user and the
 * quest's stable key (e.g. {@code "hacker"}, {@code "detective_quiz"}).
 */
@Embeddable
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSideQuestId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "quest_key", nullable = false, length = 64)
  private String questKey;
}
