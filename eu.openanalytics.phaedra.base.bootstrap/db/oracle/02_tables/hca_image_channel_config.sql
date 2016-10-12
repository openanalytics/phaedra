
-- ======================================================================= 
-- TABLE hca_image_channel_config.sql
-- ======================================================================= 

create table hca_image_channel_config (
	image_channel_id		number not null,
	setting_name			varchar2(100) not null,
	setting_value 			varchar2(500)
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_image_channel_config
	ADD CONSTRAINT hca_image_channel_config_pk
		PRIMARY KEY  ( image_channel_id, setting_name ) 
		USING INDEX TABLESPACE phaedra_i;


ALTER TABLE hca_image_channel_config
	ADD CONSTRAINT hca_image_channel_config_fk_1
		FOREIGN KEY (image_channel_id)
		REFERENCES hca_image_channel(image_channel_id)
		ON DELETE CASCADE;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_image_channel_config to phaedra_role_crud;
GRANT SELECT ON  hca_image_channel_config to phaedra_role_read;