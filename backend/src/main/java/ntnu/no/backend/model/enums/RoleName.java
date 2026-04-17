package ntnu.no.backend.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the available user roles in the system.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Getter
@RequiredArgsConstructor
public enum RoleName {
  SYS_ADMIN("sys_admin"),
  INST_ADMIN("inst_admin"),
  INSTRUCTOR("instructor"),
  STUDENT("student");

  private final String value;
}
