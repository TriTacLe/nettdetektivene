-- =============================================================
--  Repeatable Stored Procedures
-- =============================================================

-- Pin connection collation so DECLAREd locals inside SPs match the
-- utf8mb4_unicode_ci columns. Without this, MySQL 8's default
-- utf8mb4_0900_ai_ci leaks into local CHAR variables and breaks
-- equality comparisons against the table columns.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$

-- -------------------------------------------------------------
-- sp_submit_attempt
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_submit_attempt$$
CREATE PROCEDURE sp_submit_attempt(
    IN p_attempt_id   CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_user_id      CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_task_id      CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_classroom_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
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

-- -------------------------------------------------------------
-- sp_request_display_name_change
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_request_display_name_change$$
CREATE PROCEDURE sp_request_display_name_change(
    IN p_user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_requested_display_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
)
BEGIN
    DECLARE v_current_display_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_classroom_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT classroom_id, display_name
INTO v_classroom_id, v_current_display_name
FROM classroom_student
WHERE user_id = p_user_id
    LIMIT 1;

IF v_classroom_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Student is not enrolled in a classroom.';
END IF;

    IF TRIM(p_requested_display_name) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Requested display name cannot be empty.';
END IF;

    IF p_requested_display_name = v_current_display_name THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Requested display name is the same as the current display name.';
END IF;

UPDATE classroom_student
SET requested_display_name   = p_requested_display_name,
    name_change_status       = 'PENDING',
    name_change_requested_at = NOW(),
    name_change_reviewed_by  = NULL,
    name_change_reviewed_at  = NULL
WHERE user_id = p_user_id;

SELECT classroom_id,
       user_id,
       display_name,
       requested_display_name,
       name_change_status,
       name_change_requested_at,
       name_change_reviewed_by,
       name_change_reviewed_at
FROM classroom_student
WHERE user_id = p_user_id;
END$$

-- -------------------------------------------------------------
-- sp_review_display_name_change
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_review_display_name_change$$
CREATE PROCEDURE sp_review_display_name_change(
    IN p_teacher_user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_student_user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_decision VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
)
BEGIN
    DECLARE v_classroom_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_requested_display_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_pending_status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_teacher_assigned INT DEFAULT 0;

SELECT classroom_id, requested_display_name, name_change_status
INTO v_classroom_id, v_requested_display_name, v_pending_status
FROM classroom_student
WHERE user_id = p_student_user_id
    LIMIT 1;

IF v_classroom_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Student is not enrolled in a classroom.';
END IF;

    IF v_pending_status <> 'PENDING' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'There is no pending display name change request for this student.';
END IF;

SELECT COUNT(*)
INTO v_teacher_assigned
FROM classroom_instructor
WHERE classroom_id = v_classroom_id
  AND user_id = p_teacher_user_id;

IF v_teacher_assigned = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Teacher is not assigned to this classroom.';
END IF;

    IF p_decision = 'APPROVED' THEN
UPDATE classroom_student
SET display_name            = requested_display_name,
    name_change_status      = 'APPROVED',
    name_change_reviewed_by = p_teacher_user_id,
    name_change_reviewed_at = NOW()
WHERE user_id = p_student_user_id;

ELSEIF p_decision = 'REJECTED' THEN
UPDATE classroom_student
SET name_change_status      = 'REJECTED',
    name_change_reviewed_by = p_teacher_user_id,
    name_change_reviewed_at = NOW()
WHERE user_id = p_student_user_id;

ELSE
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Decision must be approved or rejected.';
END IF;

SELECT classroom_id,
       user_id,
       display_name,
       requested_display_name,
       name_change_status,
       name_change_requested_at,
       name_change_reviewed_by,
       name_change_reviewed_at
FROM classroom_student
WHERE user_id = p_student_user_id;
END$$

-- -------------------------------------------------------------
-- sp_award_achievement
-- looks up an achievement by trigger_type/trigger_ref
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_award_achievement$$
CREATE PROCEDURE sp_award_achievement(
    IN p_user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_trigger_type VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_trigger_ref VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_classroom_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
)
BEGIN
    DECLARE v_ach_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

    SELECT achievement_id INTO v_ach_id
    FROM achievement
    WHERE trigger_type = p_trigger_type AND trigger_ref = p_trigger_ref
    LIMIT 1;

    IF v_ach_id IS NOT NULL THEN
        INSERT IGNORE INTO user_achievement (user_id, achievement_id, classroom_id, earned_at)
        VALUES (p_user_id, v_ach_id, p_classroom_id, NOW());
    END IF;
END$$

DELIMITER ;