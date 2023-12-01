DROP TABLE student_cases;
DROP TABLE students;
DROP TABLE employees;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone DEFAULT NULL,
    external_id text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text,
    system_user bool DEFAULT FALSE
);
CREATE UNIQUE INDEX uniq$users$external_id ON users(external_id);

INSERT INTO users (id, external_id, first_name, last_name, email, system_user)
VALUES ('00000000-0000-0000-0000-000000000000', 'api-gw', 'api-gw', 'system-user', NULL, TRUE);

CREATE TABLE students (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id),
    updated timestamp with time zone DEFAULT NULL,
    updated_by uuid REFERENCES users(id) DEFAULT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    ssn text NOT NULL,
    date_of_birth date,
    valpas_link text NOT NULL,
    phone text NOT NULL,
    email text NOT NULL,
    address text NOT NULL
);

CREATE UNIQUE INDEX uniq$students$ssn ON students(ssn) WHERE ssn <> '';
CREATE UNIQUE INDEX uniq$students$valpas_link ON students(valpas_link) WHERE valpas_link <> '';
CREATE INDEX idx$students$first_name ON students(first_name);
CREATE INDEX idx$students$last_name ON students(last_name);

CREATE TABLE student_cases (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id),
    updated timestamp with time zone DEFAULT NULL,
    updated_by uuid REFERENCES users(id) DEFAULT NULL,
    student_id uuid NOT NULL REFERENCES students(id),
    opened_at date NOT NULL,
    assigned_to uuid REFERENCES users(id),
    info text NOT NULL
);

CREATE INDEX fk$student_cases$student_id ON student_cases(student_id);
CREATE INDEX fk$student_cases$assigned_to ON student_cases(assigned_to);
CREATE INDEX idx$student_cases$opened_at ON student_cases(opened_at);
