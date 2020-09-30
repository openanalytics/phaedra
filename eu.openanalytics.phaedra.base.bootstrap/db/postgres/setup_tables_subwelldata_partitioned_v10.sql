\set accountNameRead phaedra_role_read
\set accountNameWrite phaedra_role_crud
\set tsNameData phaedra_d
\set tsNameIndex phaedra_i

-- -----------------------------------------------------------------------
-- Table
-- -----------------------------------------------------------------------
-- Note: no constraints or indexes here
-- -----------------------------------------------------------------------

create table phaedra.hca_subwellfeature_value
	(well_id bigint, feature_id bigint, num_val float[])
	partition by range (well_id);

grant INSERT, UPDATE, DELETE on phaedra.hca_subwellfeature_value to :accountNameWrite;
grant SELECT on phaedra.hca_subwellfeature_value to :accountNameRead;

-- -----------------------------------------------------------------------
-- Partition tables
-- -----------------------------------------------------------------------

create table phaedra.hca_subwellfeature_value_part_current
	partition of phaedra.hca_subwellfeature_value
	for values from (1) to (MAXVALUE);

alter table phaedra.hca_subwellfeature_value_part_current
	add constraint hca_subwellfeature_value_part_current_pk
	primary key (well_id, feature_id)
	using index tablespace :tsNameIndex;

alter table phaedra.hca_subwellfeature_value_part_current
	add constraint hca_subwellfeature_value_part_current_fk_well
	foreign key (well_id)
	references phaedra.hca_plate_well(well_id)
	on delete cascade;

alter table phaedra.hca_subwellfeature_value_part_current
	add constraint hca_subwellfeature_value_part_current_fk_ft
	foreign key (feature_id)
	references phaedra.hca_subwellfeature(subwellfeature_id)
	on delete cascade;
	
create index hca_subwellfeature_value_part_current_ix 
	on phaedra.hca_subwellfeature_value_part_current (feature_id)
	tablespace :tsNameIndex;
	
grant INSERT, UPDATE, DELETE on phaedra.hca_subwellfeature_value_part_current to :accountNameWrite;
grant SELECT on phaedra.hca_subwellfeature_value_part_current to :accountNameRead;
