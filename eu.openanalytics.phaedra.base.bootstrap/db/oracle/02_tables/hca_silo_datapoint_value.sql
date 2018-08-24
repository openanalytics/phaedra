CREATE TABLE hca_silo_datapoint_value (
	datapoint_id		number not null,
	column_id			number not null,
	str_value			varchar2(255),
	float_value			binary_double,
	long_value			number
)
TABLESPACE phaedra_d;

ALTER TABLE hca_silo_datapoint_value
	ADD CONSTRAINT hca_silo_datapoint_value_pk
	PRIMARY KEY  ( datapoint_id, column_id ) 
	USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_datapoint_value
	ADD CONSTRAINT hca_silo_dp_value_fk_dp
	FOREIGN KEY (datapoint_id)
	REFERENCES phaedra.hca_silo_datapoint(datapoint_id)
	ON DELETE CASCADE;

ALTER TABLE hca_silo_datapoint_value
	ADD CONSTRAINT hca_silo_dp_value_fk_column
	FOREIGN KEY (column_id)
	REFERENCES phaedra.hca_silo_dataset_column(column_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON hca_silo_datapoint_value to phaedra_role_crud;
GRANT SELECT ON hca_silo_datapoint_value to phaedra_role_read;
