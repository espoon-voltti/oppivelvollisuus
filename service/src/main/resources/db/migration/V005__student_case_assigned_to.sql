ALTER TABLE student_cases
    ADD COLUMN assigned_to text REFERENCES employees(external_id);
