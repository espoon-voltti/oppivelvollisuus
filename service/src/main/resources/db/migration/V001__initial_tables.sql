CREATE TABLE students (
                         id uuid PRIMARY KEY,
                         first_name text NOT NULL,
                         last_name text NOT NULL
);

CREATE TABLE student_cases (
                         id uuid PRIMARY KEY,
                         student_id uuid REFERENCES students(id) NOT NULL,
                         opened_at date NOT NULL,
                         info text NOT NULL
);
