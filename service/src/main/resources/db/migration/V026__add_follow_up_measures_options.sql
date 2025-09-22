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

ALTER TABLE student_cases ADD CONSTRAINT check_follow_up_measure_null_or_required
    CHECK ( (finished_reason = 'COMPULSORY_EDUCATION_ENDED') = (follow_up_measures IS NOT NULL) );