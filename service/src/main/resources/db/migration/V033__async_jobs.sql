-- SPDX-FileCopyrightText: 2025-2025 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

CREATE TABLE async_job (
    id uuid DEFAULT uuid_generate_v1mc() NOT NULL PRIMARY KEY,
    type text NOT NULL,
    submitted_at timestamp with time zone DEFAULT now() NOT NULL,
    run_at timestamp with time zone DEFAULT now() NOT NULL,
    claimed_at timestamp with time zone,
    claimed_by bigint,
    retry_count integer NOT NULL,
    retry_interval interval NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    payload jsonb NOT NULL
);

CREATE INDEX idx$async_job$run_at ON async_job (run_at) WHERE (completed_at IS NULL);
CREATE INDEX idx$async_job$completed_at ON async_job (completed_at) WHERE (completed_at IS NOT NULL);

CREATE TABLE async_job_work_permit (
    pool_id text NOT NULL PRIMARY KEY,
    available_at timestamp with time zone NOT NULL
);

CREATE TABLE scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL,
    priority smallint,
    PRIMARY KEY (task_name, task_instance)
);
