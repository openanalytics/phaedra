create user phaedra password 'phaedra' admin;
create user phaedra_usr password 'phaedra_usr';

create schema PHAEDRA authorization phaedra;

runscript from 'classpath:eu/openanalytics/phaedra/base/bootstrap/db/01_roles.sql';
runscript from 'classpath:eu/openanalytics/phaedra/base/bootstrap/db/02_tables.sql';
runscript from 'classpath:eu/openanalytics/phaedra/base/bootstrap/db/03_views.sql';