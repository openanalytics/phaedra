
-- ======================================================================= 
-- TABLE query_filter
-- ======================================================================= 
-- 
-- 

create table query_filter (
	query_filter_id			number not null,
	query_id				number not null,
	type					varchar2(255),
	column_name				varchar2(100),
	positive				number(1) 	default 1,
	operator_type			varchar2(25),
	operator				varchar2(25),
	case_sensitive			number(1) 	default 0,
	value					blob	
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE query_filter
	ADD CONSTRAINT query_filter_pk
		PRIMARY KEY  ( query_filter_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE query_filter
	ADD CONSTRAINT query_filter_fk_query
		FOREIGN KEY (query_id)
		REFERENCES query(query_id)
		ON DELETE CASCADE;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE query_filter_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON query_filter to phaedra_role_crud;
GRANT SELECT ON query_filter_s to phaedra_role_crud;
GRANT SELECT ON query_filter to phaedra_role_read;
