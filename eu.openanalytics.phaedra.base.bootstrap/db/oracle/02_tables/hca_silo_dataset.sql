CREATE TABLE hca_silo_dataset (
	dataset_id				number not null,
	dataset_name			varchar2(100) not null, 
	silo_id					number not null
)
tablespace phaedra_d;

ALTER TABLE hca_silo_dataset
	ADD CONSTRAINT hca_silo_dataset_pk
	PRIMARY KEY  ( dataset_id ) 
	USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_silo_dataset
	ADD CONSTRAINT hca_silo_dataset_fk_silo
	FOREIGN KEY (silo_id)
	REFERENCES phaedra.hca_silo(silo_id)
	ON DELETE CASCADE;

CREATE SEQUENCE hca_silo_dataset_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

GRANT INSERT, UPDATE, DELETE ON hca_silo_dataset to phaedra_role_crud;
GRANT SELECT ON hca_silo_dataset_s to phaedra_role_crud;
GRANT SELECT ON hca_silo_dataset to phaedra_role_read;