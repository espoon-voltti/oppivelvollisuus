DROP TABLE student_cases;
DROP TABLE students;

CREATE TABLE students (
    id uuid PRIMARY KEY,
    created timestamp with time zone NOT NULL DEFAULT now(),
    first_name text NOT NULL,
    last_name text NOT NULL,
    ssn text NOT NULL,
    date_of_birth date,
    valpas_link text NOT NULL
);

CREATE UNIQUE INDEX uniq$students$ssn ON students(ssn) WHERE ssn <> '';
CREATE UNIQUE INDEX uniq$students$valpas_link ON students(valpas_link) WHERE valpas_link <> '';

CREATE TABLE student_cases (
    id uuid PRIMARY KEY,
    created timestamp with time zone NOT NULL DEFAULT now(),
    student_id uuid REFERENCES students(id) NOT NULL,
    opened_at date NOT NULL,
    info text NOT NULL
);

CREATE INDEX fk$student_cases$student_id ON student_cases(student_id);
CREATE INDEX fk$student_cases$opened_at ON student_cases(opened_at);
