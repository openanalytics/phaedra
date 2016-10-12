-- ======================================================================= 
-- TABLE hca_silo_group
-- ======================================================================= 

create table hca_silo_group (
	group_id				number not null,
	group_name				varchar2(100), 
	description				varchar2(200),
	protocolclass_id		number not null,
	owner					varchar2(25),
	creation_date			date,
	group_type				number not null,
	access_scope			varchar2(25),
	is_example 				number(1) 			default 0
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_silo_group
	ADD CONSTRAINT hca_silo_group_pk
		PRIMARY KEY  ( group_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_group
	ADD CONSTRAINT hca_silo_group_fk_pclass
		FOREIGN KEY (protocolclass_id)
		REFERENCES hca_protocolclass(protocolclass_id)
		ON DELETE CASCADE;

		
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_silo_group_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_silo_group to phaedra_role_crud;
GRANT SELECT ON hca_silo_group_s to phaedra_role_crud;
GRANT SELECT ON hca_silo_group to phaedra_role_read;