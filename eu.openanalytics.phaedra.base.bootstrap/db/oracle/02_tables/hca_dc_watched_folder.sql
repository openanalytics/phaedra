
-- ======================================================================= 
-- TABLE hca_dc_watched_folder
-- ======================================================================= 

create table hca_dc_watched_folder (
	folder_id				number not null, 
	location				varchar2(1000),
	capture_config			varchar2(200),
	pattern					varchar2(200),
	protocolId				number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_dc_watched_folder
	ADD CONSTRAINT hca_dc_watched_folder_pk
		PRIMARY KEY  ( folder_id ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_dc_watched_folder_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_dc_watched_folder to phaedra_role_crud;
GRANT SELECT ON  hca_dc_watched_folder_s to phaedra_role_crud;
GRANT SELECT ON  hca_dc_watched_folder to phaedra_role_read;
