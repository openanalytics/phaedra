-- ======================================================================= 
-- TABLE hca_reading
-- ======================================================================= 

create table hca_reading (
	reading_id				number not null,
	reading_dt				date,
	reading_user			varchar2(50),
	file_name				varchar2(100),
	file_part				number,
	file_info				varchar2(500),
	barcode					varchar2(50),
	plate_rows				number,
	plate_columns			number,
	src_path				varchar2(300),
	capture_path			varchar2(300),
	instrument				varchar2(150),
	protocol				varchar2(100),
	experiment				varchar2(100),
	link_dt					date,
	link_user				varchar2(50),
	link_status				number default 0
)
tablespace phaedra_d;

ALTER TABLE hca_reading
	ADD CONSTRAINT hca_reading_pk
		PRIMARY KEY  ( reading_id ) 
		USING INDEX TABLESPACE phaedra_i;

CREATE SEQUENCE hca_reading_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

GRANT INSERT, UPDATE, DELETE ON hca_reading to phaedra_role_crud;
GRANT SELECT ON hca_reading_s to phaedra_role_crud;
GRANT SELECT ON hca_reading to phaedra_role_read;