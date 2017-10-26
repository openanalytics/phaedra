-- ======================================================================= 
-- Table HCA_CLASSIFICATION
-- ======================================================================= 

create table hca_classification (
	classification_id number not null,
	pattern varchar2(100),
	pattern_type varchar2(30),
	description varchar2(300),
	rgb_color number,
	label varchar2(100),
	symbol varchar2(50),
	subwellfeature_id number,
	wellfeature_id number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_classification
	ADD CONSTRAINT hca_classification_pk
		PRIMARY KEY  ( classification_id ) 
		USING INDEX TABLESPACE phaedra_i;
    
ALTER TABLE hca_classification
	ADD CONSTRAINT hca_cf_fk_cell_feature
		FOREIGN KEY (subwellfeature_id)
		REFERENCES hca_subwellfeature(subwellfeature_id)
		ON DELETE CASCADE;

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_classification_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_classification to phaedra_role_crud;
GRANT SELECT ON  hca_classification_s to phaedra_role_crud;
GRANT SELECT ON  hca_classification to phaedra_role_read;