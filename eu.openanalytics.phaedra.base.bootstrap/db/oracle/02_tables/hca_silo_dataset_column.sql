CREATE TABLE hca_silo_dataset_column (
	column_id				number not null,
	column_name				varchar2(100) not null, 
	dataset_id				number not null,
	data_type				varchar2(25) not null
)
TABLESPACE phaedra_d;

ALTER TABLE hca_silo_dataset_column
	ADD CONSTRAINT hca_silo_dataset_column_pk
	PRIMARY KEY  ( column_id ) 
	USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_dataset_column
	ADD CONSTRAINT hca_silo_dataset_column_fk_ds
	FOREIGN KEY (dataset_id)
	REFERENCES phaedra.hca_silo_dataset(dataset_id)
	ON DELETE CASCADE;

CREATE SEQUENCE hca_silo_dataset_column_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

GRANT INSERT, UPDATE, DELETE ON hca_silo_dataset_column to phaedra_role_crud;
GRANT SELECT ON hca_silo_dataset_column_s to phaedra_role_crud;
GRANT SELECT ON hca_silo_dataset_column to phaedra_role_read;