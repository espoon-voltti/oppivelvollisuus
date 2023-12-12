ALTER TABLE students
    ADD COLUMN guardian_info text NOT NULL DEFAULT '',
    ADD COLUMN support_contacts_info text NOT NULL DEFAULT '';
ALTER TABLE students
    ALTER COLUMN guardian_info DROP DEFAULT,
    ALTER COLUMN support_contacts_info DROP DEFAULT;
