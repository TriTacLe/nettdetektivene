-- =============================================================
--  Repeatable Triggers
-- =============================================================

-- Pin connection collation so DECLAREd locals inside triggers match the
-- utf8mb4_unicode_ci columns. Without this, MySQL 8's default
-- utf8mb4_0900_ai_ci leaks into local CHAR variables and breaks
-- equality comparisons against the table columns.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

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

-- -------------------------------------------------------------
-- trg_notify_stop_completed_insert / _update
-- Notifies instructors when a student completes all tasks in a stop location.
-- Fires on both INSERT and UPDATE of task_best_score, because
-- trg_attempt_sync uses INSERT ... ON DUPLICATE KEY UPDATE — a first-attempt
-- pass is an INSERT, not an UPDATE.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_stop_completed$$
DROP TRIGGER IF EXISTS trg_notify_stop_completed_insert$$
CREATE TRIGGER trg_notify_stop_completed_insert
AFTER INSERT ON task_best_score
FOR EACH ROW
BEGIN
    DECLARE v_stop_tasks INT DEFAULT 0;
    DECLARE v_stop_passed INT DEFAULT 0;
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_stop_name VARCHAR(100) DEFAULT NULL;

    IF NEW.first_passed_at IS NOT NULL THEN
        SELECT COUNT(DISTINCT t.task_id)
        INTO v_stop_tasks
        FROM task t
        JOIN classroom_task ct ON ct.task_id = t.task_id
        WHERE t.stop_location = (SELECT stop_location FROM task WHERE task_id = NEW.task_id)
          AND ct.classroom_id = NEW.classroom_id;

        SELECT COUNT(*)
        INTO v_stop_passed
        FROM task_best_score tbs
        JOIN task t ON t.task_id = tbs.task_id
        WHERE tbs.user_id = NEW.user_id
          AND tbs.classroom_id = NEW.classroom_id
          AND t.stop_location = (SELECT stop_location FROM task WHERE task_id = NEW.task_id)
          AND tbs.first_passed_at IS NOT NULL;

        IF v_stop_passed >= v_stop_tasks AND v_stop_tasks > 0 THEN
            SELECT display_name INTO v_student_name
            FROM app_user WHERE user_id = NEW.user_id;

            SELECT stop_location INTO v_stop_name
            FROM task WHERE task_id = NEW.task_id;

            INSERT INTO notification
                (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
            SELECT
                UUID(), ci.user_id, NEW.classroom_id,
                CONCAT(v_student_name, ' har fullført ', v_stop_name, '!'),
                'stop_completed', NEW.user_id, 0, NOW()
            FROM classroom_instructor ci
            WHERE ci.classroom_id = NEW.classroom_id;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_notify_stop_completed_update$$
CREATE TRIGGER trg_notify_stop_completed_update
AFTER UPDATE ON task_best_score
FOR EACH ROW
BEGIN
    DECLARE v_stop_tasks INT DEFAULT 0;
    DECLARE v_stop_passed INT DEFAULT 0;
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_stop_name VARCHAR(100) DEFAULT NULL;

    IF NEW.first_passed_at IS NOT NULL
       AND (OLD.first_passed_at IS NULL OR OLD.first_passed_at <> NEW.first_passed_at) THEN

        SELECT COUNT(DISTINCT t.task_id)
        INTO v_stop_tasks
        FROM task t
        JOIN classroom_task ct ON ct.task_id = t.task_id
        WHERE t.stop_location = (SELECT stop_location FROM task WHERE task_id = NEW.task_id)
          AND ct.classroom_id = NEW.classroom_id;

        SELECT COUNT(*)
        INTO v_stop_passed
        FROM task_best_score tbs
        JOIN task t ON t.task_id = tbs.task_id
        WHERE tbs.user_id = NEW.user_id
          AND tbs.classroom_id = NEW.classroom_id
          AND t.stop_location = (SELECT stop_location FROM task WHERE task_id = NEW.task_id)
          AND tbs.first_passed_at IS NOT NULL;

        IF v_stop_passed >= v_stop_tasks AND v_stop_tasks > 0 THEN
            SELECT display_name INTO v_student_name
            FROM app_user WHERE user_id = NEW.user_id;

            SELECT stop_location INTO v_stop_name
            FROM task WHERE task_id = NEW.task_id;

            INSERT INTO notification
                (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
            SELECT
                UUID(), ci.user_id, NEW.classroom_id,
                CONCAT(v_student_name, ' har fullført ', v_stop_name, '!'),
                'stop_completed', NEW.user_id, 0, NOW()
            FROM classroom_instructor ci
            WHERE ci.classroom_id = NEW.classroom_id;
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_notify_game_completed
-- Notifies instructors when a student completes all tasks in a classroom
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_game_completed$$
CREATE TRIGGER trg_notify_game_completed
AFTER INSERT ON task_best_score
FOR EACH ROW
BEGIN
    DECLARE v_total_tasks INT DEFAULT 0;
    DECLARE v_passed_tasks INT DEFAULT 0;
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_classroom_title VARCHAR(255) DEFAULT NULL;

    IF NEW.first_passed_at IS NOT NULL THEN
        -- Count total active tasks in this classroom
        SELECT COUNT(*)
        INTO v_total_tasks
        FROM classroom_task
        WHERE classroom_id = NEW.classroom_id
          AND is_active = 1;

        -- Count tasks this student has passed in this classroom
        SELECT COUNT(*)
        INTO v_passed_tasks
        FROM task_best_score tbs
        JOIN classroom_task ct ON tbs.task_id = ct.task_id
        WHERE tbs.user_id = NEW.user_id
          AND ct.classroom_id = NEW.classroom_id
          AND ct.is_active = 1
          AND tbs.first_passed_at IS NOT NULL;

        -- If student has completed all tasks, notify instructors
        IF v_passed_tasks >= v_total_tasks AND v_total_tasks > 0 THEN
            SELECT u.display_name
            INTO v_student_name
            FROM app_user u
            WHERE u.user_id = NEW.user_id;

            SELECT c.title
            INTO v_classroom_title
            FROM classroom c
            WHERE c.classroom_id = NEW.classroom_id;

            INSERT INTO notification
                (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
            SELECT
                UUID(),
                ci.user_id,
                NEW.classroom_id,
                CONCAT(v_student_name, ' har fullført spillet i klasserommet "', v_classroom_title, '".'),
                'game_completed',
                NEW.user_id,
                0,
                NOW()
            FROM classroom_instructor ci
            WHERE ci.classroom_id = NEW.classroom_id;
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_notify_mystery_submitted
-- Notifies instructors when a student submits a mystery
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_mystery_submitted$$
CREATE TRIGGER trg_notify_mystery_submitted
AFTER INSERT ON mystery_submission
FOR EACH ROW
BEGIN
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;

    -- Get student display name
    SELECT display_name
    INTO v_student_name
    FROM app_user
    WHERE user_id = NEW.submitted_by;

    -- Notify all instructors of this classroom
    INSERT INTO notification
        (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
    SELECT
        UUID(),
        ci.user_id,
        NEW.classroom_id,
        CONCAT(v_student_name, ' har sendt inn et bidrag til ukens mysterium.'),
        'mystery_submission',
        NEW.submission_id,
        0,
        NOW()
    FROM classroom_instructor ci
    WHERE ci.classroom_id = NEW.classroom_id;
END$$

-- -------------------------------------------------------------
-- trg_notify_name_change_request
-- Notifies instructors when a student requests a display name change
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_name_change_request$$
CREATE TRIGGER trg_notify_name_change_request
AFTER UPDATE ON classroom_student
FOR EACH ROW
BEGIN
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_requested_name VARCHAR(255) DEFAULT NULL;
    
    IF NEW.name_change_status = 'PENDING' AND OLD.name_change_status <> 'PENDING' THEN
        -- Get student display name and requested name
        SELECT u.display_name, cs.requested_display_name
        INTO v_student_name, v_requested_name
        FROM app_user u
        INNER JOIN classroom_student cs ON u.user_id = cs.user_id
        WHERE u.user_id = NEW.user_id AND cs.classroom_id = NEW.classroom_id;
        
        -- Notify all instructors of this classroom
        INSERT INTO notification
            (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
        SELECT 
            UUID(),
            ci.user_id,
            NEW.classroom_id,
            CONCAT(v_student_name, ' ønsker å endre navnet sitt til "', v_requested_name, '".'),
            'name_change',
            NEW.user_id,
            0,
            NOW()
        FROM classroom_instructor ci
        WHERE ci.classroom_id = NEW.classroom_id;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_notify_attempt_correct
-- Notifies instructors the first time a student passes a task.
-- Uses COUNT on task_attempt (which already includes the new row)
-- to detect first-pass without relying on task_best_score order.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_attempt_correct$$
CREATE TRIGGER trg_notify_attempt_correct
AFTER INSERT ON task_attempt
FOR EACH ROW
BEGIN
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_task_title   VARCHAR(255) DEFAULT NULL;
    DECLARE v_pass_count   INT DEFAULT 0;
    DECLARE v_msg          VARCHAR(500);

    IF NEW.passed = 1 THEN
        SELECT COUNT(*)
        INTO v_pass_count
        FROM task_attempt
        WHERE user_id     = NEW.user_id
          AND task_id     = NEW.task_id
          AND classroom_id = NEW.classroom_id
          AND passed      = 1;

        IF v_pass_count = 1 THEN
            SELECT display_name INTO v_student_name
            FROM app_user WHERE user_id = NEW.user_id;

            SELECT title INTO v_task_title
            FROM task WHERE task_id = NEW.task_id;

            SET v_msg = CONCAT(v_student_name, ' fullførte oppgaven "', v_task_title, '".');

            INSERT INTO notification
                (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
            SELECT
                UUID(), ci.user_id, NEW.classroom_id,
                v_msg, 'task_attempt_correct', NEW.attempt_id, 0, NOW()
            FROM classroom_instructor ci
            WHERE ci.classroom_id = NEW.classroom_id;
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_notify_attempt_failed
-- Notifies all classroom instructors when a student fails a task attempt
-- for the first time. Subsequent failures on the same (user, task, classroom)
-- are not re-notified — the teacher already knows the student is struggling.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_notify_attempt_failed$$
CREATE TRIGGER trg_notify_attempt_failed
AFTER INSERT ON task_attempt
FOR EACH ROW
BEGIN
    DECLARE v_student_name VARCHAR(255) DEFAULT NULL;
    DECLARE v_task_title   VARCHAR(255) DEFAULT NULL;
    DECLARE v_fail_count   INT DEFAULT 0;
    DECLARE v_msg          VARCHAR(500);

    IF NEW.passed = 0 THEN
        SELECT COUNT(*)
        INTO v_fail_count
        FROM task_attempt
        WHERE user_id      = NEW.user_id
          AND task_id      = NEW.task_id
          AND classroom_id = NEW.classroom_id
          AND passed       = 0;

        IF v_fail_count = 1 THEN
            SELECT display_name INTO v_student_name
            FROM app_user WHERE user_id = NEW.user_id;

            SELECT title INTO v_task_title
            FROM task WHERE task_id = NEW.task_id;

            SET v_msg = CONCAT(v_student_name, ' feilet oppgaven "', v_task_title, '".');

            INSERT INTO notification
                (notification_id, recipient_id, classroom_id, msg, ref_type, ref_id, is_read, created_at)
            SELECT
                UUID(), ci.user_id, NEW.classroom_id,
                v_msg, 'task_attempt_failed', NEW.attempt_id, 0, NOW()
            FROM classroom_instructor ci
            WHERE ci.classroom_id = NEW.classroom_id;
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_award_achievements
-- awards achievements based on task completion criteria.
-- uses sp_award_achievement for idempotent inserts.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_award_achievements$$
DROP TRIGGER IF EXISTS trg_award_achievements_update$$

CREATE TRIGGER trg_award_achievements
AFTER INSERT ON task_best_score
FOR EACH ROW
BEGIN
    CALL sp_check_and_award_achievements(NEW.user_id, NEW.task_id, NEW.classroom_id, 1);
END$$

CREATE TRIGGER trg_award_achievements_update
AFTER UPDATE ON task_best_score
FOR EACH ROW
BEGIN
    IF OLD.first_passed_at IS NULL AND NEW.first_passed_at IS NOT NULL THEN
        CALL sp_check_and_award_achievements(NEW.user_id, NEW.task_id, NEW.classroom_id, 0);
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_award_failed_count
-- Awards the "tasks_failed/10" achievement once a user has accumulated
-- ten failed task attempts across all classrooms.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_award_failed_count$$
CREATE TRIGGER trg_award_failed_count
AFTER INSERT ON task_attempt
FOR EACH ROW
BEGIN
    DECLARE v_fail_count INT DEFAULT 0;

    IF NEW.passed = 0 THEN
        SELECT COUNT(*)
        INTO v_fail_count
        FROM task_attempt
        WHERE user_id = NEW.user_id AND passed = 0;

        IF v_fail_count >= 10 THEN
            CALL sp_award_achievement(NEW.user_id, 'tasks_failed', '10', NEW.classroom_id);
        END IF;
    END IF;
END$$

-- -------------------------------------------------------------
-- trg_unlock_creates_notebook
-- when a task is unlocked, reads its hint from the content JSON
-- and adds it to the notebook_entry for that stop.
-- -------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_unlock_creates_notebook$$
CREATE TRIGGER trg_unlock_creates_notebook
AFTER INSERT ON user_task_unlocked
FOR EACH ROW
BEGIN
    DECLARE v_stop VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
    DECLARE v_hint TEXT DEFAULT NULL;
    DECLARE v_existing_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

    SELECT stop_location, JSON_UNQUOTE(JSON_EXTRACT(content, '$.hint'))
    INTO v_stop, v_hint
    FROM task
    WHERE task_id = NEW.task_id
    LIMIT 1;

    IF v_stop IS NOT NULL THEN
        SELECT entry_id INTO v_existing_id
        FROM notebook_entry
        WHERE user_id = NEW.user_id
          AND classroom_id = NEW.classroom_id
          AND stop_location = v_stop
        LIMIT 1;

        IF v_existing_id IS NULL THEN
            INSERT INTO notebook_entry
                (entry_id, user_id, classroom_id, stop_location, auto_notes, created_at, updated_at)
            VALUES
                (UUID(), NEW.user_id, NEW.classroom_id, v_stop,
                 IF(v_hint IS NOT NULL AND v_hint <> 'null', JSON_ARRAY(v_hint), JSON_ARRAY()),
                 NOW(), NOW());
        ELSEIF v_hint IS NOT NULL AND v_hint <> 'null' THEN
            UPDATE notebook_entry
            SET auto_notes = JSON_ARRAY_APPEND(COALESCE(auto_notes, JSON_ARRAY()), '$', v_hint),
                updated_at = NOW()
            WHERE entry_id = v_existing_id;
        END IF;
    END IF;
END$$

DELIMITER ;
