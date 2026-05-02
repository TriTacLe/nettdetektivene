-- Detective Hansen has been renamed to Detective Hamster. Update the
-- existing quiz achievement description to match.

UPDATE achievement
   SET desc_ach = 'Du svarte riktig pa alle Hamsters sporsmal'
 WHERE trigger_type = 'secret'
   AND trigger_ref = 'quiz_perfect';
