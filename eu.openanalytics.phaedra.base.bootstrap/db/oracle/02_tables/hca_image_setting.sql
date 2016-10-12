-- ======================================================================= 
-- Table hca_image_setting
-- ======================================================================= 

create table hca_image_setting (
	image_setting_id number not null,
	zoom_ratio number,
	gamma number,
	pixel_size_x number,
	pixel_size_y number,
	pixel_size_z number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_image_setting
	ADD CONSTRAINT hca_image_setting_pk
		PRIMARY KEY  ( image_setting_id ) 
		USING INDEX TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_image_setting_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_image_setting to phaedra_role_crud;
GRANT SELECT ON  hca_image_setting_s to phaedra_role_crud;
GRANT SELECT ON  hca_image_setting to phaedra_role_read;