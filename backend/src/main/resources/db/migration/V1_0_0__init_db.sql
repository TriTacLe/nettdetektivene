-- =============================================================
--  Nettdetektivene – Database Schema
-- =============================================================

-- -------------------------------------------------------------
--  AUTH
-- -------------------------------------------------------------

CREATE TABLE app_user
(
    user_id             CHAR(36)     NOT NULL PRIMARY KEY,
    display_name        VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    is_active           TINYINT(1)   NOT NULL DEFAULT 1,
    global_last_seen_at DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_app_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_local_credential
(
    credential_id   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         CHAR(36)         NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    must_change_pw  TINYINT(1)       NOT NULL DEFAULT 0,
    last_changed_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until    DATETIME         NULL,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_local_cred_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_local_cred_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_refresh_token
(
    token_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    CHAR(36)        NOT NULL,
    token_hash CHAR(64)        NOT NULL,
    issued_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME        NOT NULL,
    revoked_at DATETIME        NULL,
    ip_address VARBINARY(16)   NULL,

    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    INDEX ix_refresh_token_hash (token_hash),
    INDEX ix_refresh_token_user (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_identity
(
    identity_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          CHAR(36)        NOT NULL,
    provider_name    VARCHAR(50)     NOT NULL,
    provider_user_id VARCHAR(255)    NOT NULL,
    provider_email   VARCHAR(255)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_identity_user     FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_provider_identity UNIQUE (provider_name, provider_user_id),
    INDEX ix_identity_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  ROLES
-- -------------------------------------------------------------
--  Roles: sys_admin | inst_admin | instructor | student

CREATE TABLE role
(
    role_id   CHAR(36)    NOT NULL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,

    CONSTRAINT uq_role_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role
(
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  PROFILE
-- -------------------------------------------------------------

CREATE TABLE user_profile
(
    profile_id     CHAR(36)          NOT NULL PRIMARY KEY,
    display_name   VARCHAR(255)      NOT NULL,
    user_id        CHAR(36)          NOT NULL,
    feide_id       VARCHAR(255)      NULL,
    avatar_config  JSON              NULL,
    xp             INT UNSIGNED      NOT NULL DEFAULT 0,
    xp_level       TINYINT UNSIGNED  NOT NULL DEFAULT 1,
    created_at     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_profile_user UNIQUE (user_id),
    CONSTRAINT uq_profile_feide UNIQUE (feide_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  INSTITUTION
-- -------------------------------------------------------------

CREATE TABLE institution
(
    institution_id CHAR(36)     NOT NULL PRIMARY KEY,
    inst_name      VARCHAR(255) NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  CLASSROOM
-- -------------------------------------------------------------

CREATE TABLE classroom
(
    classroom_id    CHAR(36)     NOT NULL PRIMARY KEY,
    institution_id  CHAR(36)     NOT NULL,
    title           VARCHAR(255) NOT NULL,
    class_desc      TEXT         NULL,
    join_code       VARCHAR(20)  NOT NULL,
    created_by      CHAR(36)     NOT NULL,
    is_game_enabled TINYINT(1)   NOT NULL DEFAULT 0,
    deleted_at      DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_classroom_institution FOREIGN KEY (institution_id) REFERENCES institution (institution_id),
    CONSTRAINT fk_classroom_creator     FOREIGN KEY (created_by)     REFERENCES app_user (user_id),
    CONSTRAINT uq_classroom_join_code   UNIQUE (join_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classroom_instructor
(
    classroom_id CHAR(36) NOT NULL,
    user_id      CHAR(36) NOT NULL,
    assigned_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (classroom_id, user_id),
    CONSTRAINT fk_ci_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    CONSTRAINT fk_ci_user      FOREIGN KEY (user_id)      REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classroom_student
(
    classroom_id                CHAR(36) NOT NULL,
    user_id                     CHAR(36) NOT NULL,
    display_name                VARCHAR(255) NOT NULL,
    is_allowed                  TINYINT(1) NOT NULL DEFAULT 0,

    requested_display_name      VARCHAR(255) NULL,
    name_change_status          ENUM('NONE', 'PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'NONE',
    name_change_requested_at    DATETIME NULL,
    name_change_reviewed_by     CHAR(36) NULL,
    name_change_reviewed_at     DATETIME NULL,

    joined_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (classroom_id, user_id),
    CONSTRAINT uq_classroom_student_one_classroom UNIQUE (user_id),

    CONSTRAINT fk_cs_classroom            FOREIGN KEY (classroom_id)            REFERENCES classroom (classroom_id),
    CONSTRAINT fk_cs_user                 FOREIGN KEY (user_id)                 REFERENCES app_user (user_id),
    CONSTRAINT fk_cs_name_change_reviewer FOREIGN KEY (name_change_reviewed_by) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  TASKS
-- -------------------------------------------------------------

CREATE TABLE task
(
    task_id        CHAR(36)         NOT NULL PRIMARY KEY,
    title          VARCHAR(255)     NOT NULL,
    task_type      ENUM(
                        'NEWS',
                        'PHOTO',
                        'EMAIL',
                        'PASSWORD',
                        'MARKETPLACE',
                        'SOCIAL',
                        'BOSS'
                    ) NOT NULL,
    stop_location  VARCHAR(100)     NULL,
    difficulty     TINYINT UNSIGNED NOT NULL DEFAULT 1,
    content        JSON             NOT NULL,
    owner_id       CHAR(36)         NULL,
    is_active      TINYINT(1)       NOT NULL DEFAULT 1,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_owner FOREIGN KEY (owner_id) REFERENCES app_user (user_id),
    INDEX ix_task_stop (stop_location, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classroom_task
(
    classroom_id CHAR(36)          NOT NULL,
    task_id      CHAR(36)          NOT NULL,
    sort_order   SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_active    TINYINT(1)       NOT NULL DEFAULT 1,

    PRIMARY KEY (classroom_id, task_id),
    CONSTRAINT fk_ct_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    CONSTRAINT fk_ct_task      FOREIGN KEY (task_id)      REFERENCES task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_task_unlocked
(
    user_id      CHAR(36)   NOT NULL,
    classroom_id CHAR(36)   NOT NULL,
    task_id      CHAR(36)   NOT NULL,
    unlocked_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, classroom_id, task_id),
    CONSTRAINT fk_utu_user      FOREIGN KEY (user_id)      REFERENCES app_user (user_id),
    CONSTRAINT fk_utu_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    CONSTRAINT fk_utu_task      FOREIGN KEY (task_id)      REFERENCES task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  STUDENT PROGRESS
-- -------------------------------------------------------------

CREATE TABLE task_attempt
(
    attempt_id   CHAR(36)          NOT NULL PRIMARY KEY,
    user_id      CHAR(36)          NOT NULL,
    task_id      CHAR(36)          NOT NULL,
    classroom_id CHAR(36)          NOT NULL,
    score        SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    passed       TINYINT(1)        NOT NULL DEFAULT 0,
    attempted_at DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_att_user      FOREIGN KEY (user_id)      REFERENCES app_user (user_id),
    CONSTRAINT fk_att_task      FOREIGN KEY (task_id)      REFERENCES task (task_id),
    CONSTRAINT fk_att_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    INDEX ix_att_user_task (user_id, task_id, classroom_id),
    INDEX ix_att_classroom  (classroom_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_best_score
(
    user_id           CHAR(36)          NOT NULL,
    task_id           CHAR(36)          NOT NULL,
    classroom_id      CHAR(36)          NOT NULL,
    best_score        SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    total_attempts    SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    first_passed_at   DATETIME          NULL,
    last_attempted_at DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, task_id, classroom_id),
    CONSTRAINT fk_tbs_user      FOREIGN KEY (user_id)      REFERENCES app_user (user_id),
    CONSTRAINT fk_tbs_task      FOREIGN KEY (task_id)      REFERENCES task (task_id),
    CONSTRAINT fk_tbs_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    INDEX ix_tbs_classroom (classroom_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  ACHIEVEMENTS (badges / medals)
-- -------------------------------------------------------------

CREATE TABLE achievement
(
    achievement_id CHAR(36)     NOT NULL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    desc_ach       TEXT         NULL,
    icon           TEXT         NULL,
    trigger_type   VARCHAR(50)  NULL,
    trigger_ref    VARCHAR(100) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_achievement
(
    user_id        CHAR(36) NOT NULL,
    achievement_id CHAR(36) NOT NULL,
    classroom_id   CHAR(36) NULL,
    earned_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, achievement_id),
    CONSTRAINT fk_ua_user        FOREIGN KEY (user_id)        REFERENCES app_user (user_id),
    CONSTRAINT fk_ua_achievement FOREIGN KEY (achievement_id) REFERENCES achievement (achievement_id),
    CONSTRAINT fk_ua_classroom   FOREIGN KEY (classroom_id)   REFERENCES classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  NOTEBOOK
-- -------------------------------------------------------------

CREATE TABLE notebook_entry
(
    entry_id           CHAR(36)     NOT NULL PRIMARY KEY,
    user_id            CHAR(36)     NOT NULL,
    classroom_id       CHAR(36)     NOT NULL,
    stop_location      VARCHAR(100) NOT NULL,
    auto_notes         JSON         NULL,
    student_reflection TEXT         NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_nb_user      FOREIGN KEY (user_id)      REFERENCES app_user (user_id),
    CONSTRAINT fk_nb_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    CONSTRAINT uq_nb_per_stop  UNIQUE (user_id, classroom_id, stop_location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  UKENS MYSTERIUM
-- -------------------------------------------------------------

CREATE TABLE mystery_submission
(
    submission_id  CHAR(36)         NOT NULL PRIMARY KEY,
    classroom_id   CHAR(36)         NOT NULL,
    submitted_by   CHAR(36)         NOT NULL,
    mys_desc       TEXT             NOT NULL,
    attachment_url VARCHAR(500)     NULL,
    mys_status     ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by    CHAR(36)         NULL,
    reviewed_at    DATETIME         NULL,
    is_featured    TINYINT(1)       NOT NULL DEFAULT 0,
    week_number    TINYINT UNSIGNED NULL,
    week_year      SMALLINT UNSIGNED NULL,
    submitted_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ms_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    CONSTRAINT fk_ms_submitter FOREIGN KEY (submitted_by) REFERENCES app_user (user_id),
    CONSTRAINT fk_ms_reviewer  FOREIGN KEY (reviewed_by)  REFERENCES app_user (user_id),
    INDEX ix_ms_classroom_status (classroom_id, mys_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  NOTIFICATIONS
-- -------------------------------------------------------------

CREATE TABLE notification
(
    notification_id CHAR(36)     NOT NULL PRIMARY KEY,
    recipient_id    CHAR(36)     NOT NULL,
    classroom_id    CHAR(36)     NULL,
    msg             VARCHAR(500) NOT NULL,
    ref_type        VARCHAR(50)  NULL,
    ref_id          CHAR(36)     NULL,
    is_read         TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_notif_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (classroom_id),
    INDEX ix_notif_recipient (recipient_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
--  SEED
-- -------------------------------------------------------------

INSERT INTO role (role_id, role_name) VALUES
                                          (UUID(), 'sys_admin'),
                                          (UUID(), 'inst_admin'),
                                          (UUID(), 'instructor'),
                                          (UUID(), 'student');