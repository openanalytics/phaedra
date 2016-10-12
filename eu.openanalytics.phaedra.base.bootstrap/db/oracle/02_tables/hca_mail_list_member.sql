
-- ======================================================================= 
-- TABLE hca_mail_list_member
-- ======================================================================= 

create table hca_mail_list_member (
	email_address			varchar2(200),
	list_id					number not null
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_mail_list_member
	ADD CONSTRAINT hca_mail_list_member_pk
		PRIMARY KEY  ( list_id, email_address ) 
		USING INDEX TABLESPACE phaedra_i;
	
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_mail_list_member to phaedra_role_crud;
GRANT SELECT ON  hca_mail_list_member to phaedra_role_read;
