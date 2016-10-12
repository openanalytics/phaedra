
-- ======================================================================= 
-- TABLE hca_dc_metric
-- ======================================================================= 

create table hca_dc_metric (
	metric_id				number not null,
	timestamp				timestamp not null,
	disk_usage				number,
	ram_usage				number,
	cpu_usage				number,
	dl_speed				number,
	ul_speed				number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_dc_metric
	ADD CONSTRAINT hca_dc_metric_pk
		PRIMARY KEY  ( metric_id ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_dc_metric_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_dc_metric to phaedra_role_crud;
GRANT SELECT ON  hca_dc_metric_s to phaedra_role_crud;
GRANT SELECT ON  hca_dc_metric to phaedra_role_read;
