
-- ======================================================================= 
-- TABLE hca_well 
-- ======================================================================= 
-- 
-- 

create table hca_plate_well (
	well_id				number not null,
	plate_id			number not null,
	row_nr				number not null, 
	col_nr				number not null,
	welltype_code		varchar2(10),
	concentration		number, 
	is_valid			number default 1,
	annotation_available number default 0,
	platecompound_id	number,
	description			varchar2(200)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_plate_well
	ADD CONSTRAINT hca_plate_well_pk
		PRIMARY KEY  ( well_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_plate
		FOREIGN KEY (plate_id)
		REFERENCES hca_plate(plate_id)
		ON DELETE CASCADE;

ALTER TABLE hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_welltype
		FOREIGN KEY (welltype_code)
		REFERENCES hca_welltype(welltype_code);

ALTER TABLE hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_compound
		FOREIGN KEY (platecompound_id)
		REFERENCES hca_plate_compound(platecompound_id);


CREATE INDEX hca_plate_well_ix_01 
  ON hca_plate_well (welltype_code)
  TABLESPACE phaedra_i;
 
CREATE INDEX hca_plate_well_ix_02 
  ON hca_plate_well (row_nr, col_nr)
  TABLESPACE phaedra_i;
 
CREATE INDEX hca_plate_well_ix_03 
  ON hca_plate_well (plate_id)
  TABLESPACE phaedra_i;

CREATE INDEX hca_plate_well_ix_04 
  ON HCA_PLATE_WELL (PLATECOMPOUND_ID)
  TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_plate_well_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE; 
	
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_plate_well to phaedra_role_crud;
GRANT SELECT ON  hca_plate_well_s to phaedra_role_crud;
GRANT SELECT ON  hca_plate_well to phaedra_role_read;