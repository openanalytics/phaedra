-- ======================================================================= 
-- TABLE hca_user 
-- ======================================================================= 
-- 

create table hca_user (
	user_code			VARCHAR2(25) not null, 
	email				VARCHAR2(50),
	last_logon			DATE
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_user
	ADD CONSTRAINT hca_user_pk
		PRIMARY KEY  ( user_code ) 
		USING INDEX TABLESPACE phaedra_i;


-- ======================================================================= 
-- TABLE hca_user_session
-- ======================================================================= 
-- 

create table hca_user_session (
	session_id			NUMBER not null,
	user_code			VARCHAR2(25) not null,
	login_date			DATE not null,
	host				VARCHAR2(50),
	version				VARCHAR2(50)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE hca_user_session
	ADD CONSTRAINT hca_user_session_pk
		PRIMARY KEY  ( session_id ) 
		USING INDEX TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_user_session_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_user to phaedra_role_crud;
GRANT INSERT, UPDATE, DELETE ON hca_user_session to phaedra_role_crud;
GRANT SELECT ON hca_user to phaedra_role_read;
GRANT SELECT ON hca_user_session to phaedra_role_read;
GRANT SELECT ON hca_user_session_s to phaedra_role_crud;
