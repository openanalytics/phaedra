-- ======================================================================= 
-- TABLE hca_subwellfeature
-- ======================================================================= 

create table hca_subwellfeature (
	subwellfeature_id		number not null,
	subwellfeature_name		varchar2(100)  not null,
	short_name				varchar2(36),
	protocolclass_id		number, 
	is_numeric				number default 0,
	is_logarithmic			number default 0,
	is_key					number default 1,
	calc_formula			varchar2(1000),
	description 			varchar2(250), 
	format_string			varchar2(25),
	position_role			varchar2(50)
)
tablespace phaedra_d;

ALTER TABLE hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_pk
		PRIMARY KEY  ( subwellfeature_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_fk_pc
		FOREIGN KEY (protocolclass_id)
		REFERENCES hca_protocolclass(protocolclass_id)
		ON DELETE CASCADE;

CREATE INDEX hca_subwellfeature_ix_01
	ON hca_subwellfeature (protocolclass_id)
	TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_subwellfeature_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_subwellfeature to phaedra_role_crud;
GRANT SELECT ON hca_subwellfeature_s to phaedra_role_crud;
GRANT SELECT ON hca_subwellfeature to phaedra_role_read;