CREATE TABLE hca_curve_setting_custom (
	curve_id number not null,
	setting_name varchar2(100)  not null,
	setting_value varchar2(250)
)
TABLESPACE phaedra_d;

ALTER TABLE hca_curve_setting_custom
	ADD CONSTRAINT hca_curve_setting_custom_pk
		PRIMARY KEY  ( curve_id, setting_name ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_curve_setting_custom
	ADD CONSTRAINT hca_c_s_custom_fk_curve
		FOREIGN KEY (curve_id)
		REFERENCES hca_curve(curve_id)
		ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON hca_curve_setting_custom to phaedra_role_crud;
GRANT SELECT ON  hca_curve_setting_custom to phaedra_role_read;