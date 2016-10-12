
-- ======================================================================= 
-- TABLE query_ordering
-- ======================================================================= 
-- 
-- 

create table query_ordering (
	query_ordering_id		number not null,
	query_id				number not null,
	column_name				varchar2(100),
	column_type				varchar2(25),
	ascending				number(1) 	default 1,	
	case_sensitive			number(1) 	default 0,
	ordering_index			number not null
)
tablespace phaedra_d;


-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

ALTER TABLE query_ordering
	ADD CONSTRAINT query_ordering_pk
		PRIMARY KEY  ( query_ordering_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE query_ordering
	ADD CONSTRAINT query_ordering_fk_query
		FOREIGN KEY (query_id)
		REFERENCES query(query_id)
		ON DELETE CASCADE;


-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE query_ordering_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON query_ordering to phaedra_role_crud;
GRANT SELECT ON query_ordering_s to phaedra_role_crud;
GRANT SELECT ON query_ordering to phaedra_role_read;