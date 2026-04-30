-- Adds an XP reward to every achievement so earning one bumps the user's
-- profile xp + level. Default applies retroactively to all existing rows
-- via the column default. The actual XP grant is wired through
-- trg_user_achievement_xp in R__triggers.sql.

ALTER TABLE achievement
    ADD COLUMN xp_reward INT NOT NULL DEFAULT 25;
