
-- ======================================================================= 
-- TABLE hca_feature
-- ======================================================================= 

create table hca_feature (
	feature_id						number not null,
	feature_name					varchar2(100)  not null,
	short_name						varchar2(36),
	protocolclass_id				number, 
	is_numeric						number default 0,
	is_logarithmic					number default 0, 
	is_required						number default 1,
	is_key							number default 1,
	is_uploaded 					number default 0,
	is_annotation 					number default 0,
	is_classification_restricted	number default 0,
	calc_formula					varchar2(1000),
	curve_normalization				varchar2(25) default 'NONE',
	normalization_language			varchar2(30),
	normalization_formula			varchar2(1000),
	normalization_scope				number,
	description 					varchar2(250), 
	format_string					varchar2(25),
	low_welltype					varchar2(10), 
	high_welltype					varchar2(10),
	calc_language					varchar2(30),
	calc_trigger					varchar2(30),
	calc_sequence					number
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_feature
	ADD CONSTRAINT hca_feature_pk
		PRIMARY KEY  ( feature_id ) 
		USING INDEX TABLESPACE phaedra_i;


ALTER TABLE hca_feature
	ADD CONSTRAINT hca_feature_fk_protocolclass
		FOREIGN KEY (protocolclass_id)
		REFERENCES hca_protocolclass(protocolclass_id)
		ON DELETE CASCADE;


CREATE INDEX hca_feature_ix_01
	ON hca_feature (protocolclass_id)
	TABLESPACE phaedra_i;



-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_feature_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_feature to phaedra_role_crud;
GRANT SELECT ON  hca_feature_s to phaedra_role_crud;
GRANT SELECT ON  hca_feature to phaedra_role_read;