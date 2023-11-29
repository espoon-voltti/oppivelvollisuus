CREATE TABLE employees (
    external_id text PRIMARY KEY,
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text
);
