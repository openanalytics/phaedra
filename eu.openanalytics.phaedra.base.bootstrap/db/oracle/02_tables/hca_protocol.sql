-- ======================================================================= 
-- TABLE hca_protocol
-- ======================================================================= 

create table hca_protocol (
	protocol_id				number not null,
	protocol_name			varchar2(100), 
	protocolclass_id		number,
	description				varchar2(200),
	team_code				varchar2(25) default 'NONE',
	upload_system			varchar2(25) default 'NONE',
	image_setting_id		number
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_protocol
	ADD CONSTRAINT hca_protocol_pk
		PRIMARY KEY  ( protocol_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_protocol
	ADD CONSTRAINT hca_protocol_fk_protocolclass
		FOREIGN KEY (protocolclass_id)
		REFERENCES hca_protocolclass(protocolclass_id)
		ON DELETE CASCADE;

ALTER TABLE hca_protocol
	ADD CONSTRAINT hca_prot_fk_image_settings
		FOREIGN KEY (image_setting_id)
		REFERENCES hca_image_setting(image_setting_id)
		ON DELETE SET NULL;

CREATE INDEX hca_protocol_ix_01
	ON hca_protocol (protocolclass_id)
	TABLESPACE phaedra_i;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_protocol_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_protocol to phaedra_role_crud;
GRANT SELECT ON hca_protocol_s to phaedra_role_crud;
GRANT SELECT ON hca_protocol to phaedra_role_read;