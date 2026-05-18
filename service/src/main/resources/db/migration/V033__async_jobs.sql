create table async_job
(
    id uuid default uuid_generate_v1mc() not null primary key,
    type text not null,
    submitted_at timestamp with time zone default now() not null,
    run_at timestamp with time zone default now() not null,
    claimed_at timestamp with time zone,
    claimed_by bigint,
    retry_count integer not null,
    retry_interval interval not null,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    payload jsonb not null
);

create index idx$async_job$run_at on async_job (run_at) where (completed_at IS NULL);

create index idx$async_job$completed_at on async_job (completed_at) where (completed_at IS NOT NULL);

create table async_job_work_permit
(
    pool_id text NOT NULL primary key,
    available_at timestamp with time zone not null
);

create table scheduled_tasks
(
    task_name text not null,
    task_instance text not null,
    task_data bytea,
    execution_time timestamp with time zone not null,
    picked boolean not null,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint not null,
    priority smallint,
    primary key (task_name, task_instance)
);
