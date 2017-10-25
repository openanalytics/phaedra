-- ======================================================================= 
-- Table HCA_FEATURE_VALUE
-- ======================================================================= 

CREATE TABLE hca_feature_value
(
  WELL_ID				number not null,
  FEATURE_ID			number not null,
  RAW_NUMERIC_VALUE		binary_double,
  RAW_STRING_VALUE		varchar2(400),
  NORMALIZED_VALUE		binary_double,
  CONSTRAINT hca_feature_value_iot_pk PRIMARY KEY ( WELL_ID, FEATURE_ID )
)
ORGANIZATION INDEX
PARTITION BY RANGE(WELL_ID) (
  PARTITION current_part VALUES LESS THAN (MAXVALUE)
)
TABLESPACE PHAEDRA_D;

-- -----------------------------------------------------------------------
-- Constraints
-- -----------------------------------------------------------------------

ALTER TABLE hca_feature_value
	ADD CONSTRAINT hca_feature_value_iot_well_fk
		FOREIGN KEY (well_id)
		REFERENCES hca_plate_well(well_id)
		ON DELETE CASCADE;

ALTER TABLE hca_feature_value
	ADD CONSTRAINT hca_feature_value_iot_feature_fk
		FOREIGN KEY (feature_id)
		REFERENCES hca_feature(feature_id)
		ON DELETE CASCADE;

CREATE INDEX hca_feature_value_iot_ix1 ON hca_feature_value(feature_id) ONLINE NOLOGGING LOCAL TABLESPACE phaedra_i;
		
-- -----------------------------------------------------------------------
-- Grants
-- -----------------------------------------------------------------------

GRANT INSERT, UPDATE, DELETE ON hca_feature_value to phaedra_role_crud;
GRANT SELECT ON hca_feature_value to phaedra_role_read;

-------------------------------------------------------------
-- Maintaining the partitioning
-------------------------------------------------------------

-- To split the current partition (e.g. every year or every quarter):

-- ALTER TABLE hca_feature_value_part SPLIT PARTITION current_part AT ( wellIdToSplitAt )
-- INTO (PARTITION name_for_old_part, PARTITION current_part);
