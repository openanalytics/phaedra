-- ==============================
-- Run the below script as SYSTEM
-- ==============================

create tablespace phaedra_i datafile 'phaedra_i.dbf' size 32m autoextend on maxsize 4g;
create tablespace phaedra_d datafile 'phaedra_d.dbf' size 32m autoextend on maxsize 4g;

create user phaedra identified by phaedra default tablespace phaedra_d;
create user phaedra_usr identified by phaedra_usr default tablespace phaedra_d;
create user phaedra_readonly identified by phaedra_readonly default tablespace phaedra_d;

grant connect to phaedra;
grant connect to phaedra_usr;
grant connect to phaedra_readonly;

grant create table to phaedra;
grant create view to phaedra;
grant create role to phaedra;
grant create procedure to phaedra;
grant create sequence to phaedra;
grant create database link to phaedra;
grant create session to phaedra;
grant create session to phaedra_usr;