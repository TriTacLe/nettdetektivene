DELIMITER $$

DROP PROCEDURE IF EXISTS sp_submit_attempt$$

CREATE PROCEDURE sp_submit_attempt(
    IN p_attempt_id   CHAR(36),
    IN p_user_id      CHAR(36),
    IN p_task_id      CHAR(36),
    IN p_classroom_id CHAR(36),
    IN p_score        SMALLINT UNSIGNED,
    IN p_passed       TINYINT(1)
)
BEGIN
    DECLARE v_game_enabled TINYINT DEFAULT 0;

    SELECT is_game_enabled
    INTO v_game_enabled
    FROM classroom
    WHERE classroom_id = p_classroom_id
      AND deleted_at IS NULL;

    IF v_game_enabled = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Game is currently disabled for this classroom.';
    END IF;

    START TRANSACTION;

        INSERT INTO task_attempt
            (attempt_id, user_id, task_id, classroom_id, score, passed, attempted_at)
        VALUES
            (p_attempt_id, p_user_id, p_task_id, p_classroom_id, p_score, p_passed, NOW());

    COMMIT;

    SELECT  tbs.best_score,
            tbs.total_attempts,
            tbs.first_passed_at,
            tbs.last_attempted_at,
            up.xp,
            up.xp_level
    FROM task_best_score tbs
    JOIN user_profile up ON up.user_id = tbs.user_id
    WHERE tbs.user_id = p_user_id
      AND tbs.task_id = p_task_id
      AND tbs.classroom_id = p_classroom_id;
END$$

DELIMITER ;