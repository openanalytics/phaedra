
-- ======================================================================= 
-- TABLE hca_archive_log
-- ======================================================================= 

create table hca_archive_log (
	log_id					number not null,
	log_date				timestamp not null,
	event_type				number default 0,
	message					varchar2(1000),
	error					varchar2(2000),
	job_id					varchar2(50),
	job_type				varchar2(50),
	job_source				varchar2(250),
	job_user				varchar2(50)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_archive_log
	ADD CONSTRAINT hca_archive_log_pk
		PRIMARY KEY  ( log_id ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_archive_log_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_archive_log to phaedra_role_crud;
GRANT SELECT ON  hca_archive_log_s to phaedra_role_crud;
GRANT SELECT ON  hca_archive_log to phaedra_role_read;
