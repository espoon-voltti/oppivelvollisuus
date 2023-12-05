CREATE TYPE case_event_type AS ENUM (
    'NOTE',
    'EXPLANATION_REQUEST',
    'EDUCATION_SUSPENSION_APPLICATION_RECEIVED',
    'EDUCATION_SUSPENSION_GRANTED',
    'EDUCATION_SUSPENSION_DENIED',
    'CHILD_PROTECTION_NOTICE',
    'HEARING_LETTER',
    'HEARING',
    'DIRECTED_TO_SCHOOL_TUVA',
    'DIRECTED_TO_SCHOOL_SPECIAL_EDUCATION_TUVA',
    'DIRECTED_TO_SCHOOL_SPECIAL_EDUCATION_TELMA'
);

CREATE TABLE case_events (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id),
    updated timestamp with time zone DEFAULT NULL,
    updated_by uuid REFERENCES users(id) DEFAULT NULL,
    student_case_id uuid NOT NULL REFERENCES student_cases(id),
    date date NOT NULL,
    type case_event_type NOT NULL,
    notes text NOT NULL
);

CREATE INDEX fk$case_events$student_case_id ON case_events(student_case_id);
