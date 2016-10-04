
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
