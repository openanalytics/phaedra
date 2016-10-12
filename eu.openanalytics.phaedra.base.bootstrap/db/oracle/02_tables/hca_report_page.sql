-- ======================================================================= 
-- Table HCA_REPORT_PAGE
-- ======================================================================= 

CREATE TABLE HCA_REPORT_PAGE
(
    REPORT_PAGE_ID 		NUMBER NOT NULL,
    REPORT_ID			NUMBER NOT NULL,
    PART_SETTINGS_ID	NUMBER,
	PAGE_INPUT			CLOB,
	PAGE_STYLE			NUMBER DEFAULT 0,
	PAGE_ORDER			NUMBER NOT NULL,
	PAGE_GROUP			VARCHAR2(100),
	TITLE				VARCHAR2(100),
	DESCRIPTION			VARCHAR2(200),
	CLASS_NAME			VARCHAR2(256)
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE HCA_REPORT_PAGE ADD CONSTRAINT HCA_REPORT_PAGE_PK
	PRIMARY KEY (REPORT_PAGE_ID)
	USING INDEX TABLESPACE phaedra_i;
	
ALTER TABLE HCA_REPORT_PAGE ADD CONSTRAINT FK_HCA_REPORT_PAGE_REPORT
	FOREIGN KEY (REPORT_ID) REFERENCES HCA_REPORT (REPORT_ID) ON DELETE CASCADE;
	
ALTER TABLE HCA_REPORT_PAGE ADD CONSTRAINT FK_HCA_REP_PAGE_PART_SETTINGS
	FOREIGN KEY (PART_SETTINGS_ID) REFERENCES HCA_PART_SETTINGS (SETTINGS_ID) ON DELETE CASCADE;
	
-- -----------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------

CREATE SEQUENCE HCA_REPORT_PAGE_S
	INCREMENT BY 1
	START WITH 1
	NOMAXVALUE
	NOCYCLE
	NOCACHE;

-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE on HCA_REPORT_PAGE to phaedra_role_crud;
GRANT SELECT on HCA_REPORT_PAGE_S to phaedra_role_crud;
GRANT SELECT ON HCA_REPORT_PAGE to phaedra_role_read;