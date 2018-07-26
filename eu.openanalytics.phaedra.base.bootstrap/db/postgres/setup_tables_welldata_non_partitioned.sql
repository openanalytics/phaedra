\set accountNameRead phaedra_role_read
\set accountNameWrite phaedra_role_crud
\set tsNameData phaedra_d
\set tsNameIndex phaedra_i

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_feature_value (
	well_id 			bigint not null,
	feature_id 			bigint not null,
	raw_numeric_value 	double precision,
	raw_string_value 	varchar(400),
	normalized_value 	double precision
)
TABLESPACE :tsNameData;

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_pk
	PRIMARY KEY (well_id, feature_id)
	USING INDEX TABLESPACE :tsNameIndex;

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_fk_w
	FOREIGN KEY (well_id)
	REFERENCES phaedra.hca_plate_well (well_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_fk_f
	FOREIGN KEY (feature_id)
	REFERENCES phaedra.hca_feature (feature_id)
	ON DELETE CASCADE;

CREATE INDEX hca_feature_value_ix1
	ON phaedra.hca_feature_value(feature_id)
	TABLESPACE :tsNameIndex;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_feature_value to :accountNameWrite;
GRANT SELECT ON phaedra.hca_feature_value to :accountNameRead;
