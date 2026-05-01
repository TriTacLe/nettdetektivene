-- Removes the unused "Perfekt Score" achievement (no trigger ever awarded it
-- in practice) and adds a new "Bidragsyter" achievement awarded the first
-- time a student submits a mystery. The seeder drops the old row and adds
-- the new row for fresh databases; this migration cleans up environments
-- that were seeded before the change.

DELETE FROM user_achievement
WHERE achievement_id IN (
    SELECT achievement_id FROM achievement
    WHERE trigger_type = 'perfect_score'
);

DELETE FROM achievement
WHERE trigger_type = 'perfect_score';

INSERT INTO achievement (achievement_id, title, desc_ach, icon, trigger_type, trigger_ref)
SELECT UUID(), 'Bidragsyter', 'Send inn ditt første ukens mysterium', '✍️', 'mystery_submitted', 'first'
 WHERE NOT EXISTS (
     SELECT 1 FROM achievement
      WHERE trigger_type = 'mystery_submitted' AND trigger_ref = 'first'
 );
