
-- ======================================================================= 
-- TABLE hca_feature_group
-- ======================================================================= 

create table hca_feature_group (
	group_id				number not null,
	group_name				varchar2(100) not null,
	description 			varchar2(250),
	group_type				number not null,
	protocolclass_id		number not null
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_feature_group
	ADD CONSTRAINT hca_feature_group_pk
		PRIMARY KEY  ( group_id ) 
		USING INDEX TABLESPACE phaedra_i;

		
ALTER TABLE hca_feature_group
	ADD CONSTRAINT hca_fg_fk_protocolclass
		FOREIGN KEY (protocolclass_id)
		REFERENCES hca_protocolclass(protocolclass_id)
		ON DELETE CASCADE;
		
		
ALTER TABLE hca_feature
	ADD (group_id number);
		
		
ALTER TABLE hca_subwellfeature
 	ADD (group_id number);
		
		
ALTER TABLE hca_feature
	ADD CONSTRAINT hca_feature_fk_feature_group
		FOREIGN KEY (group_id)
		REFERENCES hca_feature_group(group_id)
		ON DELETE SET NULL;


ALTER TABLE hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_fk_fg
		FOREIGN KEY (group_id)
		REFERENCES hca_feature_group(group_id)
		ON DELETE SET NULL;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_feature_group_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_feature_group to phaedra_role_crud;
GRANT SELECT ON hca_feature_group_s to phaedra_role_crud;
GRANT SELECT ON hca_feature_group to phaedra_role_read;