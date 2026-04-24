package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserTaskUnlockedId implements Serializable {

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "classroom_id")
  private UUID classroomId;

  @Column(name = "task_id")
  private UUID taskId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserTaskUnlockedId that = (UserTaskUnlockedId) o;
    return Objects.equals(userId, that.userId) &&
        Objects.equals(classroomId, that.classroomId) &&
        Objects.equals(taskId, that.taskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, classroomId, taskId);
  }
}