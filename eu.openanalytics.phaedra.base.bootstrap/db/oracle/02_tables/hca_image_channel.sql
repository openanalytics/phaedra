-- ======================================================================= 
-- Table HCA_IMAGE_CHANNEL
-- ======================================================================= 

create table hca_image_channel (
  image_channel_id number not null,
  channel_name varchar2(100),
  description varchar2(400),
  channel_type number,
  channel_sequence number,
  channel_source number,
  color_mask number,
  lookup_low number,
  lookup_high number,
  show_in_plate number(1,0),
  show_in_well number(1,0),
  alpha number,
  level_min number,
  level_max number,
  bit_depth number,
  image_setting_id number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_image_channel
	ADD CONSTRAINT hca_image_channel_pk
		PRIMARY KEY  ( image_channel_id ) 
		USING INDEX TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_image_channel_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_image_channel to phaedra_role_crud;
GRANT SELECT ON  hca_image_channel_s to phaedra_role_crud;
GRANT SELECT ON  hca_image_channel to phaedra_role_read;