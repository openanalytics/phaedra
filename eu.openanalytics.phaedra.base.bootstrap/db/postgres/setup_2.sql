-- ===============================
-- Run the below script as PHAEDRA
-- ===============================

-- psql -f setup2.sql -U phaedra postgresql://hostname:5432

CREATE SCHEMA phaedra;

\ir setup_roles.sql

GRANT USAGE ON SCHEMA phaedra TO phaedra_role_read;

\ir setup_tables.sql
\ir setup_views.sql
\ir setup_functions.sql