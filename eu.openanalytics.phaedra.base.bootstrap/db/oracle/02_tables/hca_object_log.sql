-- ======================================================================= 
-- TABLE hca_object_log
-- ======================================================================= 

create table hca_object_log (
	log_id				NUMBER not null,
	timestamp			DATE not null,
	user_code			VARCHAR2(25) not null,
	event_code			VARCHAR2(10) not null,
	object_class		VARCHAR2(50) not null,
	object_id			NUMBER not null,
	object_prop_1		VARCHAR2(50),
	object_prop_2		VARCHAR2(50),
	old_value			VARCHAR2(200),
	new_value			VARCHAR2(200),
	remark				VARCHAR2(200)
) tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_object_log
	ADD CONSTRAINT hca_object_log_pk
		PRIMARY KEY  ( log_id ) 
		USING INDEX TABLESPACE phaedra_i;

ALTER TABLE hca_object_log
	ADD CONSTRAINT hca_object_log_fk_eventtype
		FOREIGN KEY (event_code)
		REFERENCES hca_object_log_eventtype(event_code);
		
-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

CREATE INDEX hca_object_log_ix_1 ON hca_object_log(object_class) TABLESPACE phaedra_i;
CREATE INDEX hca_object_log_ix_2 ON hca_object_log(object_id) TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------

CREATE SEQUENCE hca_object_log_s
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;
	
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_object_log to phaedra_role_crud;
GRANT SELECT ON hca_object_log_s to phaedra_role_crud;
GRANT SELECT ON hca_object_log to phaedra_role_read;
