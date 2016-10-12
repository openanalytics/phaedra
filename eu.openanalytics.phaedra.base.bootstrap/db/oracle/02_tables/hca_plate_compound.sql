
-- ======================================================================= 
-- TABLE hca_plate_compound 
-- ======================================================================= 
-- 
-- 

create table hca_plate_compound (
	platecompound_id	number 			not null,			
	plate_id			number 			not null,
	compound_ty			varchar2(10) 	not null,
	compound_nr			varchar2(50) 	not null,
	validate_status		number  		default 0, 
	validate_user		varchar2(25),
	validate_dt			date, 
	upload_status		number  		default 0, 
	upload_user			varchar2(25),
	upload_dt			date,
	description			varchar2(200),
	saltform			varchar2(50)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_plate_compound
	ADD CONSTRAINT hca_plate_compound_pk
		PRIMARY KEY  ( platecompound_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_plate_compound
	ADD CONSTRAINT hca_plate_compound_fk_plate
		FOREIGN KEY (plate_id)
		REFERENCES hca_plate(plate_id)
		ON DELETE CASCADE;

CREATE INDEX hca_plate_compound_ix_01
	ON hca_plate_compound (compound_ty, compound_nr)
	TABLESPACE phaedra_i;
	
CREATE INDEX hca_plate_compound_ix_02
	ON hca_plate_compound (plate_id)
	TABLESPACE phaedra_i;


CREATE UNIQUE INDEX hca_plate_compound_uix_01
	ON hca_plate_compound (plate_id, compound_ty, compound_nr)
	TABLESPACE phaedra_i;
	
		
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_plate_compound_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE; 	
	
	
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_plate_compound to phaedra_role_crud;
GRANT SELECT ON  hca_plate_compound_s to phaedra_role_crud;
GRANT SELECT ON  hca_plate_compound to phaedra_role_read;