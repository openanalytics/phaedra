
CREATE OR REPLACE VIEW phaedra.hca_protocolclasses AS
SELECT pc.*,
	(SELECT count(protocol_id) FROM phaedra.hca_protocol WHERE protocolclass_id = pc.protocolclass_id) protocol_count,
	(SELECT count(feature_id) FROM phaedra.hca_feature WHERE protocolclass_id = pc.protocolclass_id) feature_count
FROM phaedra.hca_protocolclass pc;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_protocols AS
SELECT  p.*, pc.protocolclass_name,
	(SELECT count(protocol_id) FROM phaedra.hca_experiment WHERE protocol_id = p.protocol_id) experiment_count,
	(SELECT max(experiment_dt) FROM phaedra.hca_experiment WHERE protocol_id = p.protocol_id) last_experiment_dt
FROM phaedra.hca_protocol p, phaedra.hca_protocolclass pc
WHERE pc.protocolclass_id = p.protocolclass_id;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_experiments AS
SELECT  e.*,pr.protocol_name , pr.team_code, prc.protocolclass_id, prc.protocolclass_name, 
	 to_char(e.experiment_dt, 'YYYY.IW') week_nr, 
	(SELECT count(experiment_id) FROM phaedra.hca_plate WHERE experiment_id = e.experiment_id) plate_count
FROM phaedra.hca_experiment e, phaedra.hca_protocol pr,  phaedra.hca_protocolclass prc 
WHERE pr.protocol_id = e.protocol_id
AND prc.protocolclass_id = pr.protocolclass_id;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plates AS
SELECT p.*, e.experiment_name, e.protocol_id, e.protocol_name, e.team_code, e.protocolclass_id, e.protocolclass_name
FROM phaedra.hca_plate p, phaedra.hca_experiments e
WHERE e.experiment_id = p.experiment_id;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plate_compounds AS
SELECT c.*, p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name
FROM phaedra.hca_plate_compound c, phaedra.hca_plates p
WHERE p.plate_id = c.plate_id;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plate_wells AS
SELECT w.*, p.barcode, p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name, c.compound_ty, c.compound_nr
FROM phaedra.hca_plate_well w 
LEFT OUTER JOIN phaedra.hca_plate_compound c on w.platecompound_id = c.platecompound_id, phaedra.hca_plates p
WHERE p.plate_id = w.plate_id;

-- -----------------------------------------------------------------------

GRANT SELECT ON phaedra.hca_protocolclasses 	to phaedra_role_read;
GRANT SELECT ON phaedra.hca_protocols 			to phaedra_role_read;
GRANT SELECT ON phaedra.hca_experiments 		to phaedra_role_read;
GRANT SELECT ON phaedra.hca_plates 				to phaedra_role_read;
GRANT SELECT ON phaedra.hca_plate_compounds 	to phaedra_role_read;
GRANT SELECT ON phaedra.hca_plate_wells 		to phaedra_role_read;

-- ======================================================================= 
-- Curve views
-- ======================================================================= 

CREATE OR REPLACE VIEW phaedra.hca_curves AS
SELECT
		c.curve_id AS curve_id,
		c.feature_id AS feature_id,
		c.curve_kind AS kind,
		c.curve_method AS method,
		c.curve_model AS model,
		c.curve_type AS type,
		c.fit_date AS fit_date,
		c.fit_version AS fit_version,
		c.fit_error AS fit_error,
		c.emax AS emax,
		c.emax_conc AS emax_conc,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'PIC50' AND curve_id = c.curve_id) AS pic50,
		(SELECT string_value FROM phaedra.hca_curve_property WHERE property_name = 'PIC50_CENSOR' AND curve_id = c.curve_id) AS pic50_censor,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'PIC50_STDERR' AND curve_id = c.curve_id) AS pic50_stderr,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'R2' AND curve_id = c.curve_id) AS r2,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'HILL' AND curve_id = c.curve_id) AS hill,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'PLAC' AND curve_id = c.curve_id) AS plac,
		(SELECT string_value FROM phaedra.hca_curve_property WHERE property_name = 'PLAC_CENSOR' AND curve_id = c.curve_id) AS plac_censor,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'PLAC_THRESHOLD' AND curve_id = c.curve_id) AS plac_threshold,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'NIC' AND curve_id = c.curve_id) AS nic,
		(SELECT numeric_value FROM phaedra.hca_curve_property WHERE property_name = 'NAC' AND curve_id = c.curve_id) AS nac
FROM
		phaedra.hca_curve c;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_curve_compounds AS
SELECT
		cc.platecompound_id AS platecompound_id,
		c.feature_id AS feature_id,
		c.curve_id AS curve_id
FROM
		phaedra.hca_curve c, phaedra.hca_curve_compound cc
WHERE
		c.curve_id = cc.curve_id;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_well_curves AS
SELECT
		w.well_id AS well_id,
		cc.platecompound_id AS platecompound_id,
		c.feature_id AS feature_id,
		c.curve_id AS curve_id,
		cp1.string_value AS group1,
		cp2.string_value AS group2,
		cp3.string_value AS group3
FROM
		phaedra.hca_plate_well w, phaedra.hca_curve_compound cc, phaedra.hca_curve c
		LEFT OUTER JOIN phaedra.hca_curve_property cp1 on cp1.curve_id = c.curve_id AND cp1.property_name = 'GROUP_BY_1'
		LEFT OUTER JOIN phaedra.hca_curve_property cp2 on cp2.curve_id = c.curve_id AND cp2.property_name = 'GROUP_BY_2'
		LEFT OUTER JOIN phaedra.hca_curve_property cp3 on cp3.curve_id = c.curve_id AND cp3.property_name = 'GROUP_BY_3'
WHERE
		(cp1.string_value is null OR cp1.string_value = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_1'))
		AND (cp2.string_value is null OR cp2.string_value = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_2'))
		AND (cp3.string_value is null OR cp3.string_value = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_3'))
		AND w.platecompound_id = cc.platecompound_id AND cc.curve_id = c.curve_id;

-- -----------------------------------------------------------------------

GRANT SELECT ON phaedra.hca_curves to phaedra_role_read;
GRANT SELECT ON phaedra.hca_curve_compounds to phaedra_role_read;
GRANT SELECT ON phaedra.hca_well_curves to phaedra_role_read;

-- ======================================================================= 
-- Upload views
-- =======================================================================

CREATE OR REPLACE VIEW phaedra.HCA_UPLOAD_PLATES_TODO AS
SELECT
	p.plate_id, p.barcode, e.experiment_id, e.experiment_name, e.protocol_id, pr.protocol_name, pr.upload_system
FROM
	phaedra.hca_plate p, phaedra.hca_experiment e, phaedra.hca_protocol pr
WHERE
	e.experiment_id = p.experiment_id
	AND PR.PROTOCOL_ID = E.PROTOCOL_ID
	AND P.APPROVE_STATUS > 1
	AND P.UPLOAD_STATUS = 0; 

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.HCA_UPLOAD_COMPOUNDS_TODO AS
SELECT
	c.platecompound_id, p.plate_id, p.barcode, e.experiment_id, e.experiment_name, e.protocol_id, pr.protocol_name, pr.upload_system
FROM
	phaedra.hca_plate_compound c, phaedra.hca_plate p, phaedra.hca_experiment e, phaedra.hca_protocol pr
WHERE
	EXISTS (SELECT cc.curve_id FROM phaedra.hca_curve_compound cc WHERE cc.platecompound_id = c.platecompound_id)
	AND p.plate_id = c.plate_id
	AND p.approve_status > 1
	AND e.experiment_id = p.experiment_id
	AND PR.PROTOCOL_ID = E.PROTOCOL_ID
	AND c.validate_status >= 0
	AND c.UPLOAD_STATUS = 0;
    
-- -----------------------------------------------------------------------

GRANT SELECT ON phaedra.hca_UPLOAD_PLATES_TODO to phaedra_role_read;
GRANT SELECT ON phaedra.hca_UPLOAD_COMPOUNDS_TODO to phaedra_role_read;
