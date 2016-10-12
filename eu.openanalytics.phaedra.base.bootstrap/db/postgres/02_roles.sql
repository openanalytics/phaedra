CREATE ROLE phaedra_role_read;
CREATE ROLE phaedra_role_crud;

GRANT phaedra_role_read TO phaedra_readonly;
GRANT phaedra_role_read TO phaedra_role_crud;
GRANT phaedra_role_crud TO phaedra_usr;
