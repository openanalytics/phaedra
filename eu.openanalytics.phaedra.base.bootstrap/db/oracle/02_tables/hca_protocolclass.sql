
-- ======================================================================= 
-- TABLE hca_protocolclass
-- ======================================================================= 

create table hca_protocolclass (
	protocolclass_id			number not null,
	protocolclass_name			varchar2(100),
	description					varchar2(200),
	default_feature_id			number, 
	default_lims				varchar2(25),
	default_layout_template		varchar2(512),
	default_capture_config		varchar2(512),
	is_editable					number default 1,
	is_in_development			number default 1,
	low_welltype				varchar2(10), 
	high_welltype				varchar2(10),
	image_setting_id			number,
	is_multi_dim_subwell_data 	number(1) default 0,
	default_multiplo_method		varchar2(100),
	default_multiplo_parameter	varchar2(100)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_protocolclass
	ADD CONSTRAINT hca_protocolclass_pk
		PRIMARY KEY  ( protocolclass_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_protocolclass
	ADD CONSTRAINT hca_pclass_fk_image_settings
		FOREIGN KEY (image_setting_id)
		REFERENCES hca_image_setting(image_setting_id)
		ON DELETE SET NULL;
		
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_protocolclass_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_protocolclass to phaedra_role_crud;
GRANT SELECT ON hca_protocolclass_s to phaedra_role_crud;
GRANT SELECT ON hca_protocolclass to phaedra_role_read;