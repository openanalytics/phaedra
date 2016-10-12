-- ======================================================================= 
-- Table HCA_PART_SETTINGS
-- ======================================================================= 

CREATE TABLE HCA_PART_SETTINGS
(
    SETTINGS_ID    	NUMBER NOT NULL,
    PROTOCOL_ID 	NUMBER NOT NULL,
    USER_CODE   	VARCHAR2(20) NOT NULL,
    CLASS_NAME	 	VARCHAR2(256) NOT NULL,
    NAME		   	VARCHAR2(100) NOT NULL,
	IS_GLOBAL		NUMBER DEFAULT 0,
	IS_TEMPLATE		NUMBER DEFAULT 0,
    PROPERTIES 		CLOB
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE HCA_PART_SETTINGS ADD CONSTRAINT HCA_PART_SETTINGS_PK
	PRIMARY KEY (SETTINGS_ID)
	USING INDEX TABLESPACE phaedra_i;
	
ALTER TABLE HCA_PART_SETTINGS ADD CONSTRAINT FK_HCA_PART_SETTINGS_PROTOCOL
	FOREIGN KEY (PROTOCOL_ID) REFERENCES HCA_PROTOCOL (PROTOCOL_ID) ON DELETE CASCADE;
	
-- -----------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE HCA_PART_SETTINGS_S
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE on HCA_PART_SETTINGS to phaedra_role_crud;
GRANT SELECT on HCA_PART_SETTINGS_S to phaedra_role_crud;
GRANT SELECT ON HCA_PART_SETTINGS to phaedra_role_read;