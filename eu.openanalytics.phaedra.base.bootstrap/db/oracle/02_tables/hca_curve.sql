CREATE TABLE hca_curve (
	curve_id number not null,
	feature_id number not null,
	model_id varchar2(50) not null,
	group_by_1 varchar2(100),
	group_by_2 varchar2(100),
	group_by_3 varchar2(100),
	fit_date date not null,
	fit_version varchar2(100),
	error_code number not null,
	plot blob
)
TABLESPACE phaedra_d;

ALTER TABLE hca_curve
	ADD CONSTRAINT hca_curve_pk
		PRIMARY KEY  ( curve_id ) 
		USING INDEX TABLESPACE phaedra_i;
    
ALTER TABLE hca_curve
	ADD CONSTRAINT hca_curve_fk_feature
		FOREIGN KEY (feature_id)
		REFERENCES hca_feature(feature_id)
		ON DELETE CASCADE;
    
CREATE SEQUENCE hca_curve_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

CREATE INDEX hca_curve_ix_1 ON hca_curve(feature_id) TABLESPACE phaedra_i;

GRANT INSERT, UPDATE, DELETE ON hca_curve to phaedra_role_crud;
GRANT SELECT ON  hca_curve_s to phaedra_role_crud;
GRANT SELECT ON  hca_curve to phaedra_role_read;