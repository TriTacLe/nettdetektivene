package ntnu.no.backend.model;

/**
 * Lifecycle of a per-user side quest. Once a quest leaves {@link #ACTIVE},
 * it is terminal and never returns: {@link #COMPLETED} on a successful run,
 * {@link #ABANDONED} on a decline / fail.
 */
public enum SideQuestStatus {
  ACTIVE,
  COMPLETED,
  ABANDONED
}
