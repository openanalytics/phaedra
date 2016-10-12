
-- ======================================================================= 
-- TABLE hca_colormethod_setting
-- ======================================================================= 

create table hca_colormethod_setting (
	feature_id				number not null,
	setting_name			varchar2(100)  not null,
	setting_value 			varchar2(250)
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_colormethod_setting
	ADD CONSTRAINT hca_colormethod_setting_pk
		PRIMARY KEY  ( feature_id, setting_name ) 
		USING INDEX TABLESPACE phaedra_i;


ALTER TABLE hca_colormethod_setting
	ADD CONSTRAINT hca_cm_setting_fk_feature
		FOREIGN KEY (feature_id)
		REFERENCES hca_feature(feature_id)
		ON DELETE CASCADE;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_colormethod_setting to phaedra_role_crud;
GRANT SELECT ON  hca_colormethod_setting to phaedra_role_read;