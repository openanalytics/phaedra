CREATE TABLE hca_curve_compound (
	curve_id number not null,
  platecompound_id number not null
)
TABLESPACE phaedra_d;

ALTER TABLE hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_pk
		PRIMARY KEY  ( curve_id, platecompound_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_fk_curve
		FOREIGN KEY (curve_id)
		REFERENCES hca_curve(curve_id)
		ON DELETE CASCADE;

ALTER TABLE hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_fk_comp
		FOREIGN KEY (platecompound_id)
		REFERENCES hca_plate_compound(platecompound_id)
		ON DELETE CASCADE;

CREATE INDEX hca_curve_compound_ix_1 ON hca_curve_compound(platecompound_id) TABLESPACE phaedra_i;

GRANT INSERT, UPDATE, DELETE ON hca_curve_compound to phaedra_role_crud;
GRANT SELECT ON  hca_curve_compound to phaedra_role_read;