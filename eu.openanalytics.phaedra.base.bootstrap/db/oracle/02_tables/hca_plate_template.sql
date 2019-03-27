CREATE TABLE hca_plate_template (
	template_id			number not null,
	protocolclass_id	number not null,
	template_name		varchar2(1000) not null,
	rows				number not null,
	columns				number not null,
	creator				varchar2(200),
	data_xml			XMLType	
)
tablespace phaedra_d
xmltype data_xml STORE AS CLOB;

ALTER TABLE hca_plate_template
	ADD CONSTRAINT hca_plate_template_pk
	PRIMARY KEY  ( template_id )
	USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_plate_template
	ADD CONSTRAINT hca_plate_template_fk_pc
	FOREIGN KEY (protocolclass_id)
	REFERENCES hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE SEQUENCE hca_plate_template_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_plate_template to phaedra_role_crud;
GRANT SELECT, USAGE ON phaedra.hca_plate_template_s to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_plate_template to phaedra_role_read;