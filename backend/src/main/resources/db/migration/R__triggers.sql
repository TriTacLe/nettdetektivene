-- =============================================================
--  Repeatable Triggers
-- =============================================================

DELIMITER $$

-- -------------------------------------------------------------
-- trg_classroom_auto_instructor
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_classroom_auto_instructor$$
CREATE TRIGGER trg_classroom_auto_instructor
AFTER INSERT ON classroom
FOR EACH ROW
BEGIN
    INSERT INTO classroom_instructor (classroom_id, user_id, assigned_at)
    VALUES (NEW.classroom_id, NEW.created_by, NOW());
END$$

-- -------------------------------------------------------------
-- trg_student_join_unlock_first_task
-- Unlocks the first active task in a classroom when a student joins.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_student_join_unlock_first_task$$
CREATE TRIGGER trg_student_join_unlock_first_task
AFTER INSERT ON classroom_student
FOR EACH ROW
BEGIN
    DECLARE v_first_task_id CHAR(36) DEFAULT NULL;

    SELECT task_id
    INTO v_first_task_id
    FROM classroom_task
    WHERE classroom_id = NEW.classroom_id
      AND is_active = 1
    ORDER BY sort_order ASC
    LIMIT 1;

    IF v_first_task_id IS NOT NULL THEN
        INSERT IGNORE INTO user_task_unlocked
            (user_id, classroom_id, task_id, unlocked_at)
        VALUES
            (NEW.user_id, NEW.classroom_id, v_first_task_id, NOW());
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_attempt_sync
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_attempt_sync$$
CREATE TRIGGER trg_attempt_sync
AFTER INSERT ON task_attempt
FOR EACH ROW
BEGIN
    DECLARE v_existing_first_passed_at DATETIME DEFAULT NULL;
    DECLARE v_xp_per_task INT DEFAULT 10;
    DECLARE v_current_sort_order SMALLINT;
    DECLARE v_next_task_id CHAR(36) DEFAULT NULL;

    SELECT first_passed_at
    INTO v_existing_first_passed_at
    FROM task_best_score
    WHERE user_id = NEW.user_id
      AND task_id = NEW.task_id
      AND classroom_id = NEW.classroom_id
    LIMIT 1;

    INSERT INTO task_best_score
        (user_id, task_id, classroom_id,
         best_score, total_attempts, first_passed_at, last_attempted_at)
    VALUES
        (NEW.user_id, NEW.task_id, NEW.classroom_id,
         NEW.score,
         1,
         IF(NEW.passed = 1, NOW(), NULL),
         NOW())
    ON DUPLICATE KEY UPDATE
        best_score        = IF(NEW.score > best_score, NEW.score, best_score),
        total_attempts    = total_attempts + 1,
        first_passed_at   = IF(NEW.passed = 1 AND first_passed_at IS NULL, NOW(), first_passed_at),
        last_attempted_at = NOW();

    IF NEW.passed = 1 AND v_existing_first_passed_at IS NULL THEN
        UPDATE user_profile
        SET
            xp = xp + v_xp_per_task,
            xp_level = CASE
                WHEN xp + v_xp_per_task <  50  THEN 1
                WHEN xp + v_xp_per_task <  150 THEN 2
                WHEN xp + v_xp_per_task <  300 THEN 3
                WHEN xp + v_xp_per_task <  500 THEN 4
                ELSE 5
            END
        WHERE user_id = NEW.user_id;

        SELECT sort_order
        INTO v_current_sort_order
        FROM classroom_task
        WHERE classroom_id = NEW.classroom_id
          AND task_id = NEW.task_id
        LIMIT 1;

        SELECT task_id
        INTO v_next_task_id
        FROM classroom_task
        WHERE classroom_id = NEW.classroom_id
          AND sort_order > v_current_sort_order
          AND is_active = 1
        ORDER BY sort_order ASC
        LIMIT 1;

        IF v_next_task_id IS NOT NULL THEN
            INSERT IGNORE INTO user_task_unlocked
                (user_id, classroom_id, task_id, unlocked_at)
            VALUES
                (NEW.user_id, NEW.classroom_id, v_next_task_id, NOW());
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_mystery_approved
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_mystery_approved$$
CREATE TRIGGER trg_mystery_approved
AFTER UPDATE ON mystery_submission
FOR EACH ROW
BEGIN
    DECLARE v_achievement_id CHAR(36) DEFAULT NULL;

    IF NEW.mys_status = 'APPROVED' AND OLD.mys_status <> 'APPROVED' THEN

        INSERT INTO notification
            (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
        VALUES
            (UUID(), NEW.submitted_by, NEW.classroom_id,
             'Bidraget ditt ble godkjent av læreren! Du har fått en medalje.',
             'mystery_submission', NEW.submission_id, 0, NOW());

         SELECT achievement_id
        INTO v_achievement_id
        FROM achievement
        WHERE trigger_type = 'ukens_mysterium'
        LIMIT 1;

        IF v_achievement_id IS NOT NULL THEN
            INSERT IGNORE INTO user_achievement
                (user_id, achievement_id, classroom_id, earned_at)
            VALUES
                (NEW.submitted_by, v_achievement_id, NEW.classroom_id, NOW());
        END IF;

    END IF;
END$$

DELIMITER ;
