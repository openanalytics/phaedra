
-- ======================================================================= 
-- TABLE hca_welltype
-- ======================================================================= 
-- 
-- 


create table hca_welltype (
	welltype_code			varchar2(10) not null,
	description				varchar2(100)
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_welltype
	ADD CONSTRAINT hca_welltype_pk
		PRIMARY KEY  ( welltype_code ) 
		USING INDEX TABLESPACE phaedra_i;

		
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_welltype to phaedra_role_crud;
GRANT SELECT ON hca_welltype to phaedra_role_read;


-- -----------------------------------------------------------------------
-- default values
-- -----------------------------------------------------------------------

INSERT INTO hca_welltype(welltype_code, description) VALUES ('EMPTY', 'Empty');
INSERT INTO hca_welltype(welltype_code, description) VALUES ('LC', 'Low Control');
INSERT INTO hca_welltype(welltype_code, description) VALUES ('HC', 'High Control');
INSERT INTO hca_welltype(welltype_code, description) VALUES ('SAMPLE', 'Sample');
COMMIT;