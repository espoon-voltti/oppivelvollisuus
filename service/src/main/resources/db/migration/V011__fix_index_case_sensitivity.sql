-- reindex as lowercase
DROP INDEX uniq$students$ssn;
DROP INDEX uniq$students$valpas_link;
CREATE UNIQUE INDEX uniq$students$ssn ON students(lower(ssn)) WHERE ssn <> '';
CREATE UNIQUE INDEX uniq$students$valpas_link ON students(lower(valpas_link)) WHERE valpas_link <> '';

-- remove as unnecessary
DROP INDEX idx$students$first_name;
DROP INDEX idx$students$last_name;
