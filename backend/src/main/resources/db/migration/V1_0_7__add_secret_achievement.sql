-- Hidden achievement awarded when the student catches the hacker
-- snooping around the game map. Awarded by the application (not a
-- DB trigger) since the trigger is a low-probability client event.

INSERT INTO achievement (achievement_id, title, desc_ach, icon, trigger_type, trigger_ref)
SELECT UUID(), 'Den unnvikende hackeren', 'Du tok hackeren på fersken', '🕵', 'secret', 'caught_hacker'
 WHERE NOT EXISTS (
     SELECT 1 FROM achievement
      WHERE trigger_type = 'secret' AND trigger_ref = 'caught_hacker'
 );
