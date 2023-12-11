CREATE TYPE case_status AS ENUM (
    'TODO',
    'ON_HOLD',
    'FINISHED'
);

ALTER TABLE student_cases ADD COLUMN status case_status NOT NULL DEFAULT 'FINISHED';
ALTER TABLE student_cases ALTER COLUMN status DROP DEFAULT;

CREATE INDEX idx$student_cases$status ON student_cases(status);
CREATE UNIQUE INDEX uniq$student_cases$one_unfinished_per_student
    ON student_cases(student_id) WHERE (status IN ('TODO', 'ON_HOLD'));
