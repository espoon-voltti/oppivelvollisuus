ALTER TABLE users RENAME COLUMN first_name TO first_names;
ALTER TABLE users ADD COLUMN first_name text NOT NULL
    GENERATED ALWAYS AS ( split_part(first_names, ' ', 1) ) STORED;
