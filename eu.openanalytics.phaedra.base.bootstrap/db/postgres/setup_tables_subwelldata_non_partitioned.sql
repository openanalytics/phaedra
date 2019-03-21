\set accountNameRead phaedra_role_read
\set accountNameWrite phaedra_role_crud
\set tsNameData phaedra_d
\set tsNameIndex phaedra_i

-- -----------------------------------------------------------------------
-- Table
-- -----------------------------------------------------------------------

create table phaedra.hca_subwellfeature_value
	(well_id bigint, feature_id bigint, num_val float[])
	tablespace :tsNameData;

alter table phaedra.hca_subwellfeature_value
	add constraint hca_subwellfeature_value_pk
	primary key (well_id, feature_id)
	using index tablespace :tsNameIndex;

alter table phaedra.hca_subwellfeature_value
	add constraint hca_subwellfeature_value_fk_well
	foreign key (well_id)
	references phaedra.hca_plate_well(well_id)
	on delete cascade;

alter table phaedra.hca_subwellfeature_value
	add constraint hca_subwellfeature_value_fk_ft
	foreign key (feature_id)
	references phaedra.hca_subwellfeature(subwellfeature_id)
	on delete cascade;

grant INSERT, UPDATE, DELETE on phaedra.hca_subwellfeature_value to :accountNameWrite;
grant SELECT on phaedra.hca_subwellfeature_value to :accountNameRead;