#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE DATABASE oppivelvollisuus_it;
	GRANT ALL PRIVILEGES ON DATABASE oppivelvollisuus_it TO oppivelvollisuus;
EOSQL
