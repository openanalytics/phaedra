
-- ======================================================================= 
-- TABLE hca_mail_distribution_list
-- ======================================================================= 

create table hca_mail_distribution_list (
	list_id					number not null,
	list_name				varchar2(100),
	label					varchar2(200)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_mail_distribution_list
	ADD CONSTRAINT hca_mail_distribution_list_pk
		PRIMARY KEY  ( list_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_mail_distribution_list
	ADD CONSTRAINT hca_mail_distribution_list_unq
		UNIQUE ( list_name ) 
		USING INDEX TABLESPACE phaedra_i;
		
-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_mail_distribution_list_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_mail_distribution_list to phaedra_role_crud;
GRANT SELECT ON  hca_mail_distribution_list_s to phaedra_role_crud;
GRANT SELECT ON  hca_mail_distribution_list to phaedra_role_read;
