-- ======================================================================= 
-- TABLE hca_silo_group_member
-- ======================================================================= 

create table hca_silo_group_member (
	silo_id					number not null,
	group_id				number not null
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_pk
		PRIMARY KEY  ( silo_id, group_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_fk_silo
		FOREIGN KEY (silo_id)
		REFERENCES hca_silo(silo_id)
		ON DELETE CASCADE;


ALTER TABLE hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_fk_group
		FOREIGN KEY (group_id)
		REFERENCES hca_silo_group(group_id)
		ON DELETE CASCADE;


-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_silo_group_member to phaedra_role_crud;
GRANT SELECT ON hca_silo_group_member to phaedra_role_read;