package ntnu.no.backend.model;

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

import java.time.LocalDateTime;

/**
 * @author tri
 * @since 0.1
 * @version 0.1
 */
@Entity
@Table(name = "institution")
@AllArgsConstructor
@NoArgsConstructor
@AttributeOverride(name="id", column = @Column(name = "institution_id", updatable = false, nullable = false))
@Setter
@Getter
@SuperBuilder
public class Institution extends BaseEntity{

  @Column(name = "inst_name", nullable = false)
  private String instName;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
