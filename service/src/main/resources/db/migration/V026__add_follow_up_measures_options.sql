CREATE TYPE follow_up_measure AS ENUM (
    'KELA_REHABILITATION_SERVICES',
    'SOCIAL_SERVICES',
    'YOUTH_WORK',
    'JOB_SEARCH_SUPPORT',
    'LANGUAGE_COURSE',
    'MISSING',
    'MOVE_ABROAD'
);

ALTER TABLE student_cases ADD COLUMN follow_up_measures follow_up_measure[];

/* Set existing rows to empty to avoid violating the new constraint */
UPDATE student_cases SET follow_up_measures = '{}'::follow_up_measure[] WHERE finished_reason = 'COMPULSORY_EDUCATION_ENDED';

/* Add constraint that follow_up_measures is required if and only if finished_reason is COMPULSORY_EDUCATION_ENDED */
ALTER TABLE student_cases ADD CONSTRAINT check_follow_up_measure_null_or_required
    CHECK ( (finished_reason = 'COMPULSORY_EDUCATION_ENDED') = (follow_up_measures IS NOT NULL) );
