-- ================================
-- Run the below script as POSTGRES
-- ================================

-- To execute the script:
-- psql -f setup1.sql -U postgres postgresql://hostname:5432

create role phaedra login createrole password 'phaedra';
create role phaedra_usr login password 'phaedra_usr';
create role phaedra_readonly login password 'phaedra_readonly';

create tablespace phaedra_d owner phaedra location '/usr/local/pgsql/data/phaedra_d';
create tablespace phaedra_i owner phaedra location '/usr/local/pgsql/data/phaedra_i';

create database phaedra owner phaedra tablespace phaedra_d;