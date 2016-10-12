


-- ======================================================================= 
-- TABLE hca_preference
-- ======================================================================= 
-- 
--

create table hca_preference (
	pref_type			varchar2(25),
	pref_user			varchar2(25),
	pref_item			varchar2(200),
	pref_value			clob
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_preference
	ADD CONSTRAINT hca_preference_pk
		PRIMARY KEY  ( pref_type, pref_user, pref_item ) 
		USING INDEX TABLESPACE phaedra_i;

GRANT INSERT, UPDATE, DELETE ON hca_preference to phaedra_role_crud;
GRANT SELECT ON hca_preference to phaedra_role_read;