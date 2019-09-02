\set accountNameRead phaedra_role_read

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_protocolclasses AS
SELECT pc.*,
	(SELECT count(protocol_id) FROM phaedra.hca_protocol WHERE protocolclass_id = pc.protocolclass_id) protocol_count,
	(SELECT count(feature_id) FROM phaedra.hca_feature WHERE protocolclass_id = pc.protocolclass_id) feature_count
FROM phaedra.hca_protocolclass pc;

GRANT SELECT ON phaedra.hca_protocolclasses to :accountNameRead;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_protocols AS
SELECT  p.*, pc.protocolclass_name,
	(SELECT count(protocol_id) FROM phaedra.hca_experiment WHERE protocol_id = p.protocol_id) experiment_count,
	(SELECT max(experiment_dt) FROM phaedra.hca_experiment WHERE protocol_id = p.protocol_id) last_experiment_dt
FROM phaedra.hca_protocol p, phaedra.hca_protocolclass pc
WHERE pc.protocolclass_id = p.protocolclass_id;

GRANT SELECT ON phaedra.hca_protocols to :accountNameRead;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_experiments AS
SELECT  e.*,pr.protocol_name , pr.team_code, prc.protocolclass_id, prc.protocolclass_name,
	 to_char(e.experiment_dt, 'YYYY.IW') week_nr,
	(SELECT count(experiment_id) FROM phaedra.hca_plate WHERE experiment_id = e.experiment_id) plate_count
FROM phaedra.hca_experiment e, phaedra.hca_protocol pr,  phaedra.hca_protocolclass prc
WHERE pr.protocol_id = e.protocol_id
AND prc.protocolclass_id = pr.protocolclass_id;

GRANT SELECT ON phaedra.hca_experiments to :accountNameRead;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plates AS
SELECT p.*, e.experiment_name, e.protocol_id, e.protocol_name, e.team_code, e.protocolclass_id, e.protocolclass_name
FROM phaedra.hca_plate p, phaedra.hca_experiments e
WHERE e.experiment_id = p.experiment_id;

GRANT SELECT ON phaedra.hca_plates to :accountNameRead;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plate_compounds AS
SELECT c.*, p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name
FROM phaedra.hca_plate_compound c, phaedra.hca_plates p
WHERE p.plate_id = c.plate_id;

GRANT SELECT ON phaedra.hca_plate_compounds to :accountNameRead;

-- -----------------------------------------------------------------------

CREATE OR REPLACE VIEW phaedra.hca_plate_wells AS
SELECT w.*, p.barcode, p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name, c.compound_ty, c.compound_nr
FROM phaedra.hca_plate_well w
LEFT OUTER JOIN phaedra.hca_plate_compound c on w.platecompound_id = c.platecompound_id, phaedra.hca_plates p
WHERE p.plate_id = w.plate_id;

GRANT SELECT ON phaedra.hca_plate_wells to :accountNameRead;

-- =======================================================================
-- Curve views
-- =======================================================================

CREATE OR REPLACE VIEW phaedra.hca_well_curves AS
SELECT
		w.well_id AS well_id,
		cc.platecompound_id AS platecompound_id,
		c.feature_id AS feature_id,
		c.curve_id AS curve_id,
		c.group_by_1 AS group1,
		c.group_by_2 AS group2,
		c.group_by_3 AS group3
FROM
		phaedra.hca_plate_well w, phaedra.hca_curve_compound cc, phaedra.hca_curve c
WHERE
		(c.group_by_1 is null OR c.group_by_1 = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_1'))
		AND (c.group_by_2 is null OR c.group_by_2 = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_2'))
		AND (c.group_by_3 is null OR c.group_by_3 = (SELECT case when f.is_numeric then cast(fv.raw_numeric_value AS text) else fv.raw_string_value end FROM phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			WHERE fv.well_id = w.well_id AND fv.feature_id = f.feature_id AND f.feature_name = cs.setting_value AND cs.feature_id = c.feature_id AND cs.setting_name = 'GROUP_BY_3'))
		AND w.platecompound_id = cc.platecompound_id AND cc.curve_id = c.curve_id;

GRANT SELECT ON phaedra.hca_well_curves to :accountNameRead;

-- =======================================================================
-- Upload views
-- =======================================================================

create or replace view phaedra.hca_upload_compounds_todo as
	select pc.platecompound_id
	from phaedra.hca_plate_compound pc, phaedra.hca_plate p, phaedra.hca_experiment e, phaedra.hca_protocol pr
	where pc.plate_id = p.plate_id and p.experiment_id = e.experiment_id and e.protocol_id = pr.protocol_id
	and pc.validate_status >= 0 and pc.upload_status = 0 and p.approve_status > 1 and p.upload_status = 0 and pr.upload_system is not null;

grant select on phaedra.hca_upload_compounds_todo to :accountNameRead;
grant select on phaedra.hca_upload_compounds_todo to :accountNameRead;

-- -----------------------------------------------------------------------

create or replace view phaedra.hca_upload_wells_todo as
	select w.well_id, w.plate_id, w.platecompound_id, w.concentration, w.is_valid
	from phaedra.hca_plate_well w, phaedra.hca_upload_compounds_todo cu
	where w.platecompound_id = cu.platecompound_id and w.is_valid not in (-1, -2, -8);

grant select on phaedra.hca_upload_wells_todo to :accountNameRead;
grant select on phaedra.hca_upload_wells_todo to :accountNameRead;