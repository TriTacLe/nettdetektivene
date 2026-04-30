-- Reworks the weekly mystery feature.
--
-- The old shape (mys_desc + attachment_url + is_featured + week_number/year)
-- is replaced with a structured submission: image_path + 3 alternatives +
-- correct_index. A new mystery_completion table tracks who solved what.
-- The legacy "Ukens Mysterium" achievement is repurposed to award the
-- solver, not the submitter, and a second tier is added.

-- Wipe stale rows that won't fit the new schema.
DELETE FROM notification WHERE ref_type = 'mystery_submission';
DELETE FROM mystery_submission;

-- Drop columns that no longer apply.
ALTER TABLE mystery_submission
    DROP COLUMN mys_desc,
    DROP COLUMN attachment_url,
    DROP COLUMN is_featured,
    DROP COLUMN week_number,
    DROP COLUMN week_year;

-- Add the new columns.
ALTER TABLE mystery_submission
    ADD COLUMN image_path     VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    ADD COLUMN alternative_1  TEXT         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    ADD COLUMN alternative_2  TEXT         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    ADD COLUMN alternative_3  TEXT         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    ADD COLUMN correct_index  TINYINT UNSIGNED NOT NULL,
    ADD INDEX  ix_ms_submitted_by (submitted_by);

-- Track which student solved which submission (correct answer once is enough).
CREATE TABLE mystery_completion
(
    user_id       CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    submission_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    completed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, submission_id),
    CONSTRAINT fk_mc_user       FOREIGN KEY (user_id)       REFERENCES app_user (user_id)            ON DELETE CASCADE,
    CONSTRAINT fk_mc_submission FOREIGN KEY (submission_id) REFERENCES mystery_submission (submission_id) ON DELETE CASCADE,
    INDEX ix_mc_user (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Repurpose the legacy "Ukens Mysterium" achievement (was awarded to the
-- submitter) into "Første mysterium løst" — awarded to the solver.
UPDATE achievement
   SET title        = 'Første mysterium løst',
       desc_ach     = 'Løs ditt første ukens mysterium',
       trigger_type = 'mystery_solved',
       trigger_ref  = 'first'
 WHERE trigger_type = 'ukens_mysterium';

-- Second tier: 5 mysteries solved.
INSERT INTO achievement (achievement_id, title, desc_ach, icon, trigger_type, trigger_ref)
SELECT UUID(), 'Fem mysterier løst', 'Løs fem ukens mysterier', '🏅', 'mystery_solved', '5'
 WHERE NOT EXISTS (
     SELECT 1 FROM achievement
      WHERE trigger_type = 'mystery_solved' AND trigger_ref = '5'
 );
