create role phaedra_role_read;
create role phaedra_role_crud;

grant phaedra_role_read to phaedra_readonly;
grant phaedra_role_read to phaedra_role_crud;
grant phaedra_role_crud to phaedra_usr;

alter user phaedra_usr default role connect, phaedra_role_crud;