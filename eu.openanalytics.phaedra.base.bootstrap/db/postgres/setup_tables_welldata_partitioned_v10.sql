\set accountNameRead phaedra_role_read
\set accountNameWrite phaedra_role_crud
\set tsNameData phaedra_d
\set tsNameIndex phaedra_i

-- -----------------------------------------------------------------------
-- Table
-- -----------------------------------------------------------------------
-- Note: no constraints or indexes here
-- -----------------------------------------------------------------------

create table phaedra.hca_feature_value (
	well_id 			bigint not null,
	feature_id 			bigint not null,
	raw_numeric_value 	double precision,
	raw_string_value 	varchar(400),
	normalized_value 	double precision
)
partition by range (well_id);

grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value to :accountNameWrite;
grant SELECT on phaedra.hca_feature_value to :accountNameRead;

-- -----------------------------------------------------------------------
-- Partitions
-- -----------------------------------------------------------------------

create table phaedra.hca_feature_value_part_current
	partition of phaedra.hca_feature_value
	for values from (1) to (MAXVALUE);

alter table phaedra.hca_feature_value_part_current
	add constraint hca_feature_value_part_current_pk
	primary key (well_id, feature_id)
	using index tablespace :tsNameIndex;

alter table phaedra.hca_feature_value_part_current
	add constraint hca_feature_value_part_current_fk_w
	foreign key (well_id)
	references phaedra.hca_plate_well (well_id)
	on delete cascade;

alter table phaedra.hca_feature_value_part_current
	add constraint hca_feature_value_part_current_fk_f
	foreign key (feature_id)
	references phaedra.hca_feature (feature_id)
	on delete cascade;

create index hca_feature_value_part_current_ix 
	on phaedra.hca_feature_value_part_current (feature_id)
	tablespace :tsNameIndex;

grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value_part_current to :accountNameWrite;
grant SELECT on phaedra.hca_feature_value_part_current to :accountNameRead;

-- -----------------------------------------------------------------------
-- To split a partition
-- -----------------------------------------------------------------------

--alter table phaedra.hca_feature_value
--	detach partition hca_feature_value_part_current;

--alter table phaedra.hca_feature_value_part_current rename to hca_feature_value_part_xxx;

--create table phaedra.hca_feature_value_part_current
--	partition of phaedra.hca_feature_value
--	for values from bbb to 9223372036854775807;
	
-- Add constraints and index here

--alter table phaedra.hca_feature_value
--	attach partition hca_feature_value_part_xxx
--	for values from aaa to bbb;