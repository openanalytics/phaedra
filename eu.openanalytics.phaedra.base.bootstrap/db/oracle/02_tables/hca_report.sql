-- ======================================================================= 
-- Table HCA_REPORT
-- ======================================================================= 

CREATE TABLE HCA_REPORT
(
    REPORT_ID    		NUMBER NOT NULL,
    PROTOCOL_ID 		NUMBER NOT NULL,
    USER_CODE   		VARCHAR2(20) NOT NULL,
    NAME		   		VARCHAR2(100) NOT NULL,
	DESCRIPTION			VARCHAR2(300),
	IS_GLOBAL			NUMBER DEFAULT 0,
	IS_TEMPLATE			NUMBER DEFAULT 0,
	TEMPLATE_STYLE		NUMBER DEFAULT 0,
    PAGE_SIZE			VARCHAR2(6) NOT NULL,
	PAGE_ORIENTATION	VARCHAR2(12) NOT NULL
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE HCA_REPORT ADD CONSTRAINT HCA_REPORT_PK
	PRIMARY KEY (REPORT_ID)
	USING INDEX TABLESPACE phaedra_i;
	
ALTER TABLE HCA_REPORT ADD CONSTRAINT FK_HCA_REPORT_PROTOCOL
	FOREIGN KEY (PROTOCOL_ID) REFERENCES HCA_PROTOCOL (PROTOCOL_ID) ON DELETE CASCADE;
	
-- -----------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE HCA_REPORT_S
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE on HCA_REPORT to phaedra_role_crud;
GRANT SELECT on HCA_REPORT_S to phaedra_role_crud;
GRANT SELECT ON HCA_REPORT to phaedra_role_read;