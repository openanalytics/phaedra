-- ======================================================================= 
-- Table HCA_FEATURE_VALUE
-- ======================================================================= 

CREATE TABLE hca_feature_value
(
  WELL_ID,
  FEATURE_ID,
  RAW_NUMERIC_VALUE,
  RAW_STRING_VALUE,
  NORMALIZED_VALUE,
  CONSTRAINT hca_feature_value_iot_pk PRIMARY KEY ( WELL_ID, FEATURE_ID )
)
ORGANIZATION INDEX
PARTITION BY RANGE(WELL_ID) (
  PARTITION historic_part VALUES LESS THAN (6221369),
  PARTITION part_q1_2014 VALUES LESS THAN (6661021),
  PARTITION part_q2_2014 VALUES LESS THAN (7561983),
  PARTITION part_q3_2014 VALUES LESS THAN (8617223),
  PARTITION part_q4_2014 VALUES LESS THAN (9451921),
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

-- Each quarter, do this:
--ALTER TABLE hca_feature_value_part SPLIT PARTITION current_part AT ( lastWellIdOfQuarter+1 )
--INTO (PARTITION part_qx_2015, PARTITION current_part);
