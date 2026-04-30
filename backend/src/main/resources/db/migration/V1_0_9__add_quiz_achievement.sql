-- Hidden achievement awarded when the student completes Hansen's quiz
-- with all answers correct. Awarded by the application via the side-quest
-- system on its single-stage completion path.

INSERT INTO achievement (achievement_id, title, desc_ach, icon, trigger_type, trigger_ref)
SELECT UUID(), 'Kvisseksperten', 'Du svarte riktig pa alle Hansens sporsmal', '🧠', 'secret', 'quiz_perfect'
 WHERE NOT EXISTS (
     SELECT 1 FROM achievement
      WHERE trigger_type = 'secret' AND trigger_ref = 'quiz_perfect'
 );
