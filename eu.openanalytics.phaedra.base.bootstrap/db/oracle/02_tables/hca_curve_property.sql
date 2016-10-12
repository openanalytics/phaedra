CREATE TABLE hca_curve_property (
	curve_id number not null,
	property_name varchar2(100) not null,
	numeric_value binary_double,
	string_value varchar2(150),
	binary_value blob
)
TABLESPACE phaedra_d;

ALTER TABLE hca_curve_property
	ADD CONSTRAINT hca_curve_property_pk
		PRIMARY KEY  ( curve_id, property_name ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_curve_property
	ADD CONSTRAINT hca_curve_property_fk_curve
		FOREIGN KEY (curve_id)
		REFERENCES hca_curve(curve_id)
		ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON hca_curve_property to phaedra_role_crud;
GRANT SELECT ON  hca_curve_property to phaedra_role_read;