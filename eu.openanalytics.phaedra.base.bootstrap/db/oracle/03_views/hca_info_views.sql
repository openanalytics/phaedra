
-- ======================================================================= 
-- View HCA_PROTOCOLCLASSES
-- ======================================================================= 

create or replace view hca_protocolclasses as
select pc.*,
	(select count(protocol_id) from hca_protocol where protocolclass_id = pc.protocolclass_id) protocol_count,
	(select count(feature_id) from hca_feature where protocolclass_id = pc.protocolclass_id) feature_count
from hca_protocolclass pc;

-- ======================================================================= 
-- View HCA_PROTOCOLS
-- ======================================================================= 

create or replace view hca_protocols as
select  p.*, pc.protocolclass_name,
	(select count(protocol_id) from hca_experiment where protocol_id = p.protocol_id) experiment_count,
	(select max(experiment_dt) from hca_experiment where protocol_id = p.protocol_id) last_experiment_dt
from hca_protocol p,  hca_protocolclass pc
where pc.protocolclass_id = p.protocolclass_id;

-- ======================================================================= 
-- View HCA_EXPERIMENTS 
-- ======================================================================= 

create or replace view hca_experiments as
select  e.*, pr.protocol_name , pr.team_code, prc.protocolclass_id, prc.protocolclass_name, 
	 to_char(e.experiment_dt, 'YYYY.IW') week_nr, 
	(select count(experiment_id) from hca_plate where experiment_id = e.experiment_id) plate_count
from hca_experiment e,  hca_protocol pr,  hca_protocolclass prc 
where pr.protocol_id = e.protocol_id
and prc.protocolclass_id = pr.protocolclass_id;

-- ======================================================================= 
-- View HCA_PLATES
-- ======================================================================= 

create or replace view hca_plates as
select p.*, e.experiment_name, e.protocol_id, e.protocol_name, e.team_code, e.protocolclass_id, e.protocolclass_name
from hca_plate p, hca_experiments e
where e.experiment_id = p.experiment_id;

-- ======================================================================= 
-- View HCA_PLATE_COMPOUNDS
-- ======================================================================= 

create or replace view hca_plate_compounds as
select c.*,	p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name
from hca_plate_compound c, hca_plates p
where p.plate_id = c.plate_id;

-- ======================================================================= 
-- View HCA_PLATE_WELLS
-- ======================================================================= 

create or replace view hca_plate_wells as
select w.*, p.barcode, p.experiment_id, p.experiment_name, p.protocol_id, p.protocol_name, p.team_code , p.protocolclass_id, p.protocolclass_name, c.compound_ty, c.compound_nr
from hca_plate_well w, hca_plates p, hca_plate_compound c
where p.plate_id = w.plate_id and w.platecompound_id = c.platecompound_id(+);

-- ======================================================================= 
-- View HCA_WELL_CURVES
-- =======================================================================

create or replace view phaedra.hca_well_curves as
select
		w.well_id as well_id,
		cc.platecompound_id as platecompound_id,
		c.feature_id as feature_id,
		c.curve_id as curve_id,
		c.group_by_1 as group1,
		c.group_by_2 as group2,
		c.group_by_3 as group3
from
		phaedra.hca_plate_well w, phaedra.hca_curve_compound cc, phaedra.hca_curve c
where
		(c.group_by_1 is null or c.group_by_1 = (select case when f.is_numeric = 1 then to_char(fv.raw_numeric_value) else fv.raw_string_value end from phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			where fv.well_id = w.well_id and fv.feature_id = f.feature_id and f.feature_name = cs.setting_value and cs.feature_id = c.feature_id and cs.setting_name = 'GROUP_BY_1'))
		and (c.group_by_2 is null or c.group_by_2 = (select case when f.is_numeric = 1 then to_char(fv.raw_numeric_value) else fv.raw_string_value end from phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			where fv.well_id = w.well_id and fv.feature_id = f.feature_id and f.feature_name = cs.setting_value and cs.feature_id = c.feature_id and cs.setting_name = 'GROUP_BY_2'))
		and (c.group_by_3 is null or c.group_by_3 = (select case when f.is_numeric = 1 then to_char(fv.raw_numeric_value) else fv.raw_string_value end from phaedra.hca_feature_value fv, phaedra.hca_feature f, phaedra.hca_curve_setting cs
			where fv.well_id = w.well_id and fv.feature_id = f.feature_id and f.feature_name = cs.setting_value and cs.feature_id = c.feature_id and cs.setting_name = 'GROUP_BY_3'))
		and w.platecompound_id = cc.platecompound_id and cc.curve_id = c.curve_id;

-- -----------------------------------------------------------------------
-- grants
-- -----------------------------------------------------------------------

grant select on hca_protocolclasses 	to phaedra_role_read;
grant select on hca_protocols 			to phaedra_role_read;
grant select on hca_experiments 		to phaedra_role_read;
grant select on hca_plates 				to phaedra_role_read;
grant select on hca_plate_compounds 	to phaedra_role_read;
grant select on hca_plate_wells 		to phaedra_role_read;
grant select on phaedra.hca_well_curves to phaedra_role_read;