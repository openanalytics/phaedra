
-- ======================================================================= 
-- TABLE hca_dc_log
-- ======================================================================= 

create table hca_dc_log (
	log_id					number not null,
	log_date				timestamp not null,
	log_source				varchar2(100),
	status_code				number default 0,
	message					varchar2(1000),
	error					varchar2(2000),
	source_path				varchar2(200),
	source_identifier		varchar2(200),
	reading					varchar2(200),
	task_id					varchar2(50),
	task_user				varchar2(50)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_dc_log
	ADD CONSTRAINT hca_dc_log_pk
		PRIMARY KEY  ( log_id ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_dc_log_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_dc_log to phaedra_role_crud;
GRANT SELECT ON  hca_dc_log_s to phaedra_role_crud;
GRANT SELECT ON  hca_dc_log to phaedra_role_read;
