-- ======================================================================= 
-- TABLE hca_object_log_eventtype
-- ======================================================================= 

create table hca_object_log_eventtype (
	event_code			VARCHAR2(10) not null,
	event_label			VARCHAR2(25) not null,
	event_description	VARCHAR2(200)
) tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_object_log_eventtype
	ADD CONSTRAINT hca_object_log_eventtype_pk
		PRIMARY KEY  ( event_code ) 
		USING INDEX TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------
-- indexes
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- sequence
-- -----------------------------------------------------------------------
	
-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_object_log_eventtype to phaedra_role_crud;
GRANT SELECT ON hca_object_log_eventtype to phaedra_role_read;

-- -----------------------------------------------------------------------
-- default event types
-- -----------------------------------------------------------------------

INSERT INTO hca_object_log_eventtype(event_code, event_label, event_description) VALUES ('CHANGED','Object Changed','A field or property of the object has been changed.');
INSERT INTO hca_object_log_eventtype(event_code, event_label, event_description) VALUES ('REMOVED','Object Removed','The object has been removed.');