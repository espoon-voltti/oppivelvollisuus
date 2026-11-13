ALTER TABLE students DROP COLUMN partner_organisations;
DROP TYPE partner_organisation;

ALTER TABLE student_cases DROP CONSTRAINT check_follow_up_measure_null_or_required;
ALTER TABLE student_cases DROP COLUMN follow_up_measures;
DROP TYPE follow_up_measure;
