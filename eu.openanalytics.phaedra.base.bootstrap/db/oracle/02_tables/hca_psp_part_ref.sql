

-- ======================================================================= 
-- TABLE hca_psp 
-- ======================================================================= 

create table hca_psp_part_ref (
	part_ref_id			number not null,
	psp_id				number not null,
	part_id 			varchar2(100) not null,
	part_secondary_id	varchar2(100),
	part_settings_id	number
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_pk
		PRIMARY KEY  (part_ref_id) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_fk_psp
		FOREIGN KEY (psp_id)
		REFERENCES hca_psp(psp_id)
		ON DELETE CASCADE;

ALTER TABLE hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_fk_part_sett
		FOREIGN KEY (part_settings_id)
		REFERENCES hca_part_settings(settings_id)
		ON DELETE CASCADE;

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_psp_part_ref_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_psp_part_ref to phaedra_role_crud;
GRANT SELECT ON hca_psp_part_ref_s to phaedra_role_crud;
GRANT SELECT ON hca_psp_part_ref to phaedra_role_read;