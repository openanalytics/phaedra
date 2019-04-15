create or replace function phaedra.prepare_upload (uSystem text) returns void as
$$
declare
	pId bigint;
	fId bigint;
begin
	delete from phaedra.hca_upload;
	delete from phaedra.hca_upload_point;
	delete from phaedra.hca_upload_result;

	-- Insert compounds
	insert into phaedra.hca_upload
	select
		pc.platecompound_id, pc.compound_ty, pc.compound_nr, pr.protocol_id, pr.protocol_name,
		e.experiment_id, e.experiment_name, e.experiment_dt, e.experiment_user,
		p.plate_id, p.barcode, p.description, p.plate_info, NULL, uSystem
	from
		phaedra.hca_plate_compound pc, phaedra.hca_plate p, phaedra.hca_experiment e, phaedra.hca_protocol pr
	where
		pc.plate_id = p.plate_id and p.experiment_id = e.experiment_id and e.protocol_id = pr.protocol_id
		and pc.validate_status >= 0 and pc.upload_status = 0 and p.approve_status > 1 and p.upload_status = 0
		and pr.upload_system = uSystem;

	-- Add plate properties via temp table (for performance: xpath once per plate)
	drop table if exists hca_upload_plate;
	create temp table hca_upload_plate (plate_id bigint, data_xml text);
	insert into hca_upload_plate select distinct(plate_id), NULL from phaedra.hca_upload;
	update hca_upload_plate up set data_xml = (xpath('/data/properties', p.data_xml))[1] from phaedra.hca_plate p where up.plate_id = p.plate_id;
	update phaedra.hca_upload u set data_xml = up.data_xml from hca_upload_plate up where up.plate_id = u.plate_id;

	-- Insert data points
	for fId in (select distinct f.feature_id
		from phaedra.hca_feature f, phaedra.hca_protocol p, phaedra.hca_plate_wells w, phaedra.hca_upload_wells_todo wu
		where f.protocolclass_id = p.protocolclass_id and p.upload_system = uSystem and f.is_uploaded = TRUE
		and p.protocol_id = w.protocol_id and w.well_id = wu.well_id) loop
			for pId in (select distinct plate_id from phaedra.hca_upload_wells_todo wu) loop
				insert into phaedra.hca_upload_point
				select w.well_id, w.platecompound_id, NULL, fv.feature_id, NULL, NULL, NULL, NULL, w.concentration, w.is_valid,
					fv.raw_numeric_value, fv.normalized_value
				from phaedra.hca_feature_value fv, phaedra.hca_upload_wells_todo w
				where fv.well_id = w.well_id and w.plate_id = pId and fv.feature_id = fId;
			end loop;
	end loop;
	
	-- Add curve properties
	update phaedra.hca_upload_point up
		set curve_id = wc.curve_id, group1 = wc.group1, group2 = wc.group2, group3 = wc.group3, feature_name = NULL
		from phaedra.hca_well_curves wc where up.well_id = wc.well_id and up.feature_id = wc.feature_id;
	-- Add feature names
	update phaedra.hca_upload_point up
		set feature_name = f.feature_name
		from phaedra.hca_feature f where up.feature_id = f.feature_id;

	-- Add curve results
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL, 'KIND', NULL, case when c.model_id like 'PL%' then 'OSB' else 'PLAC' end
		from phaedra.hca_curve c where c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point);
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL, 'MODEL', NULL, c.model_id
		from phaedra.hca_curve c where c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point);
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL, 'TYPE', NULL, case when csc.setting_value is null then cs.setting_value else csc.setting_value end
		from phaedra.hca_curve c left outer join phaedra.hca_curve_setting_custom csc on c.curve_id = csc.curve_id and csc.setting_name = 'Type', phaedra.hca_curve_setting cs
		where c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point) and c.feature_id = cs.feature_id and cs.setting_name = 'Type';
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL, 'PLAC_THRESHOLD', NULL, case when csc.setting_value is null then cs.setting_value else csc.setting_value end
		from phaedra.hca_curve c left outer join phaedra.hca_curve_setting_custom csc on c.curve_id = csc.curve_id and csc.setting_name = 'Threshold', phaedra.hca_curve_setting cs
		where c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point) and c.feature_id = cs.feature_id and cs.setting_name = 'Threshold';
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL as feature_name, upper(cp.property_name) as result_type, NULL as qualifier,
		cast(round(cast(cp.numeric_value as numeric), 4) as text) as value
		from phaedra.hca_curve c, phaedra.hca_curve_property cp where c.curve_id = cp.curve_id
		and c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point)
		and cp.property_name not in ('pLAC Censor', 'pIC50 Censor', 'Confidence Band', 'Weights', 'Method Fallback');
	-- Add method
	insert into phaedra.hca_upload_result
		select c.curve_id, c.feature_id, NULL as feature_name, 'METHOD', NULL as qualifier, cp.string_value as value
		from phaedra.hca_curve c, phaedra.hca_curve_property cp where c.curve_id = cp.curve_id
		and c.curve_id in (select distinct(curve_id) from phaedra.hca_upload_point)
		and cp.property_name = 'Method Fallback';
	-- Add censors
	update phaedra.hca_upload_result ur
		set qualifier = cp.string_value
		from phaedra.hca_curve_property cp where ur.curve_id = cp.curve_id and ur.result_type = 'PIC50' and cp.property_name = 'pIC50 Censor';
	update phaedra.hca_upload_result ur
		set qualifier = cp.string_value
		from phaedra.hca_curve_property cp where ur.curve_id = cp.curve_id and ur.result_type = 'PLAC' and cp.property_name = 'pLAC Censor';
	-- Add feature names
	update phaedra.hca_upload_result ur
		set feature_name = f.feature_name
		from phaedra.hca_feature f where ur.feature_id = f.feature_id;
end
$$ language plpgsql;

grant execute on function phaedra.prepare_upload to phaedraprod;