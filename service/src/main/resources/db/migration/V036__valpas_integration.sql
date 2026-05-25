ALTER TABLE students ADD COLUMN valpas_oppija_oid text NULL;
CREATE UNIQUE INDEX uniq$students$valpas_oppija_oid
    ON students(valpas_oppija_oid) WHERE valpas_oppija_oid IS NOT NULL;

ALTER TABLE student_cases ADD COLUMN valpas_notification_id uuid NULL;
CREATE UNIQUE INDEX uniq$student_cases$valpas_notification_id
    ON student_cases(valpas_notification_id) WHERE valpas_notification_id IS NOT NULL;

CREATE UNIQUE INDEX uniq$student_cases$one_imported_from_valpas_per_student
    ON student_cases(student_id) WHERE status = 'IMPORTED_FROM_VALPAS';

CREATE TYPE valpas_query_run_state AS ENUM
    ('STARTED', 'FILES_READY', 'COMPLETED', 'FAILED');

CREATE TABLE valpas_query_runs (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    external_query_id text NOT NULL,
    state valpas_query_run_state NOT NULL,
    started_polling_at timestamp with time zone NOT NULL,
    started_downloading_at timestamp with time zone,
    finished_at timestamp with time zone,
    file_urls text[],

    CONSTRAINT file_urls_consistent_with_downloading CHECK (
        (file_urls IS NULL) = (started_downloading_at IS NULL)
    ),
    CONSTRAINT state_consistency CHECK (
        CASE state
            WHEN 'STARTED'     THEN file_urls IS NULL     AND finished_at IS NULL
            WHEN 'FILES_READY' THEN file_urls IS NOT NULL AND finished_at IS NULL
            WHEN 'COMPLETED'   THEN file_urls IS NOT NULL AND finished_at IS NOT NULL
            WHEN 'FAILED'      THEN finished_at IS NOT NULL
        END
    )
);

CREATE UNIQUE INDEX uniq$valpas_query_runs$external_query_id
    ON valpas_query_runs(external_query_id);
CREATE INDEX idx$valpas_query_runs$started_polling_at
    ON valpas_query_runs(started_polling_at DESC);

ALTER TABLE student_cases DROP CONSTRAINT check_source_valpas_required_or_null;
ALTER TABLE student_cases ADD CONSTRAINT check_source_valpas_required_or_null
    CHECK (
        status = 'IMPORTED_FROM_VALPAS'
        OR (source = 'VALPAS_NOTICE') = (source_valpas IS NOT NULL)
    );

ALTER INDEX uniq$student_cases$one_unfinished_per_student
    RENAME TO uniq$student_cases$one_active_per_student;
