-- Per-user state for branching one-shot side quests on the game map.
-- Each row is one quest's progress for one user. stage is monotonic and
-- status is terminal once it leaves 'active'.

CREATE TABLE user_side_quest
(
    user_id    CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    quest_key  VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    stage      INT       NOT NULL DEFAULT 0,
    status     ENUM('active', 'completed', 'abandoned') NOT NULL DEFAULT 'active',
    updated_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, quest_key),
    CONSTRAINT fk_user_side_quest_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
