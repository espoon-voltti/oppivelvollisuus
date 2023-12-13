ALTER TABLE student_cases ADD COLUMN source_contact text NOT NULL DEFAULT '';
ALTER TABLE student_cases ALTER COLUMN source_contact DROP DEFAULT;
