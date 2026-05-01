-- Some environments ended up with multiple "Fem mysterier løst" rows
-- because both DataSeeder.java and the V1_0_5 migration could insert it
-- under slightly different trigger_ref values across history. Collapse
-- them into a single canonical row (mystery_solved / 5) and redirect
-- any user_achievement grants to the keeper so students don't lose
-- their earned medal.

SET @keeper := (
    SELECT achievement_id
      FROM achievement
     WHERE title = 'Fem mysterier løst'
     ORDER BY
       (trigger_type = 'mystery_solved' AND trigger_ref = '5') DESC,
       achievement_id ASC
     LIMIT 1
);

-- Drop user grants pointing at duplicates that would collide with an
-- existing grant on the keeper.
DELETE ua
  FROM user_achievement ua
  JOIN achievement a ON a.achievement_id = ua.achievement_id
  JOIN user_achievement keeper_ua
    ON keeper_ua.user_id = ua.user_id
   AND keeper_ua.achievement_id = @keeper
 WHERE a.title = 'Fem mysterier løst'
   AND a.achievement_id <> @keeper;

-- Redirect remaining duplicate grants to the keeper.
UPDATE user_achievement ua
  JOIN achievement a ON a.achievement_id = ua.achievement_id
   SET ua.achievement_id = @keeper
 WHERE a.title = 'Fem mysterier løst'
   AND a.achievement_id <> @keeper;

-- Delete the duplicate achievement rows.
DELETE FROM achievement
 WHERE title = 'Fem mysterier løst'
   AND achievement_id <> @keeper;

-- Make sure the keeper has the canonical trigger.
UPDATE achievement
   SET trigger_type = 'mystery_solved',
       trigger_ref  = '5'
 WHERE achievement_id = @keeper;
