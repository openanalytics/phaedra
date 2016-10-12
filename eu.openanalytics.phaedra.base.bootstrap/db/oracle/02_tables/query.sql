
-- ======================================================================= 
-- TABLE query
-- ======================================================================= 
-- 
-- 


create table query (
	query_id				number not null,
	query_name				varchar2(100),
	description				varchar2(255),
	remark					varchar2(255),
	query_user				varchar2(25),
	query_dt				date, 
	is_public				number(1) 	default 0,
	example					number(1) 	default 0,
	type					varchar2(255),
	max_results_set			number(1) 	default 1,
	max_results				number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE query
	ADD CONSTRAINT query_pk
		PRIMARY KEY  ( query_id ) 
		USING INDEX TABLESPACE phaedra_i;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE query_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON query to phaedra_role_crud;
GRANT SELECT ON query_s to phaedra_role_crud;
GRANT SELECT ON query to phaedra_role_read;