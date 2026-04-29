-- Removes location_completed achievements whose stops no longer have content.
-- The seeders (DataSeeder.java, scripts/factory.sh) already drop these for
-- fresh databases; this migration cleans up environments that were seeded
-- before the removal.

DELETE FROM user_achievement
WHERE achievement_id IN (
    SELECT achievement_id FROM achievement
    WHERE trigger_type = 'location_completed'
      AND trigger_ref IN ('postkontoret', 'passordbanken', 'markedsplassen', 'sosialen')
);

DELETE FROM achievement
WHERE trigger_type = 'location_completed'
  AND trigger_ref IN ('postkontoret', 'passordbanken', 'markedsplassen', 'sosialen');
