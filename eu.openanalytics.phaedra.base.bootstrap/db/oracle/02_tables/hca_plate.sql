

-- ======================================================================= 
-- TABLE hca_plate 
-- ======================================================================= 

create table hca_plate (
	plate_id			number not null,
	experiment_id		number not null, 
	sequence_in_run		number not null,
	barcode 			varchar2(200),
	barcode_source		varchar2(25),
	description			varchar2(200),
	plate_info 			varchar2(100),
	link_status 		number 		default 0, 
	link_user			varchar2(25),
	link_dt				date, 
	calc_status			number 		default 0,
	calc_error			varchar2(50),
	calc_dt				date, 
	validate_status		number  	default 0, 
	validate_user		varchar2(25),
	validate_dt			date, 
	approve_status		number  	default 0, 
	approve_user		varchar2(25),
	approve_dt			date, 
	upload_status		number  	default 0, 
	upload_user			varchar2(25),
	upload_dt			date, 
	jpx_available		number(1) 	default 0,
	jpx_path			varchar(20), 
	celldata_available	number(1) 	default 0,
	data_xml			XMLType,
	plate_rows			number,
	plate_columns 		number
)
tablespace phaedra_d
xmltype data_xml STORE AS CLOB;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_plate
	ADD CONSTRAINT hca_plate_pk
		PRIMARY KEY  ( plate_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_plate
	ADD CONSTRAINT hca_plate_fk_experiment
		FOREIGN KEY (experiment_id)
		REFERENCES hca_experiment(experiment_id);

CREATE INDEX hca_plate_ix_01
	ON hca_plate (barcode)
	TABLESPACE phaedra_i;

CREATE INDEX hca_plate_ix_02
	ON hca_plate (experiment_id)
	TABLESPACE phaedra_i;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_plate_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_plate to phaedra_role_crud;
GRANT SELECT ON  hca_plate_s to phaedra_role_crud;
GRANT SELECT ON  hca_plate to phaedra_role_read;