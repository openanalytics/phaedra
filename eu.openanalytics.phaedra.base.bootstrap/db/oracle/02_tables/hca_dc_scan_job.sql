
-- ======================================================================= 
-- TABLE hca_dc_scan_job
-- ======================================================================= 

create table hca_dc_scan_job (
	job_id					number not null, 
	schedule				varchar2(100),
	scanner_type			varchar2(200),
	label					varchar2(100),
	description				varchar2(1000),
	config					XMLType 
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_dc_scan_job
	ADD CONSTRAINT hca_dc_scan_job_pk
		PRIMARY KEY  ( job_id ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_dc_scan_job_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_dc_scan_job to phaedra_role_crud;
GRANT SELECT ON  hca_dc_scan_job_s to phaedra_role_crud;
GRANT SELECT ON  hca_dc_scan_job to phaedra_role_read;
