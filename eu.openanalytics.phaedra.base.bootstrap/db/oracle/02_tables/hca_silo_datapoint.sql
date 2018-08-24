CREATE TABLE hca_silo_datapoint (
	datapoint_id		number not null,
	dataset_id			number not null,
	well_id				number not null,
	subwell_id			number not null
)
TABLESPACE phaedra_d;

ALTER TABLE hca_silo_datapoint
	ADD CONSTRAINT hca_silo_datapoint_pk
	PRIMARY KEY  ( datapoint_id ) 
	USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_datapoint
	ADD CONSTRAINT hca_silo_datapoint_fk_dataset
	FOREIGN KEY (dataset_id)
	REFERENCES phaedra.hca_silo_dataset(dataset_id)
	ON DELETE CASCADE;

CREATE SEQUENCE hca_silo_datapoint_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

GRANT INSERT, UPDATE, DELETE ON hca_silo_datapoint to phaedra_role_crud;
GRANT SELECT ON hca_silo_datapoint_s to phaedra_role_crud;
GRANT SELECT ON hca_silo_datapoint to phaedra_role_read;
