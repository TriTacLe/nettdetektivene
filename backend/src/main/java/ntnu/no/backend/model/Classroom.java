package ntnu.no.backend.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import ntnu.no.backend.model.auth.User;
import java.time.LocalDateTime;

/**
 * @author tri
 * @since 0.1
 * @version 0.1
 */
@Entity
@Table(name = "classroom")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@AttributeOverride(name="id", column = @Column(name = "classroom_id"))
public class Classroom extends BaseEntity{

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @Column(nullable = false)
  private String title;

  @Column(name= "class_desc",nullable = true)
  private String classDesc;

  @Column(name = "join_code", nullable = false)
  private String joinCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false, updatable = false)
  private User createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "is_game_enabled", nullable = false)
  private boolean isGameEnabled = false;

  @Column(name = "deleted_at", nullable = true)
  private LocalDateTime deletedAt;

  @UpdateTimestamp
  @Column(name ="updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
