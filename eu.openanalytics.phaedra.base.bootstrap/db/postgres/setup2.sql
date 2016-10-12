-- ===============================
-- Run the below script as PHAEDRA
-- ===============================

-- psql.exe -f setup2.sql -U phaedra postgresql://hostname:5432

CREATE SCHEMA phaedra;

\ir 01_public_functions.sql
\ir 02_roles.sql

GRANT USAGE ON SCHEMA phaedra TO phaedra_role_read;

\ir 03_tables.sql
\ir 04_views.sql