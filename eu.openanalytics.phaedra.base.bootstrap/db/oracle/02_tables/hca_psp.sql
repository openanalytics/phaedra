

-- ======================================================================= 
-- TABLE hca_psp 
-- ======================================================================= 

create table hca_psp (
	psp_id				number not null,
	psp_name 			varchar2(100),
	workbench_state		clob,
	owner 				varchar2(50),
	access_scope 		varchar2(50),
	feature_id			number
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_psp
	ADD CONSTRAINT hca_psp_pk
		PRIMARY KEY  ( psp_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_psp
	ADD CONSTRAINT hca_psp_fk_feature
		FOREIGN KEY (feature_id)
		REFERENCES hca_feature(feature_id);


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_psp_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_psp to phaedra_role_crud;
GRANT SELECT ON hca_psp_s to phaedra_role_crud;
GRANT SELECT ON hca_psp to phaedra_role_read;