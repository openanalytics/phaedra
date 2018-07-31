\set accountNameRead phaedra_role_read
\set accountNameWrite phaedra_role_crud
\set tsNameData phaedra_d
\set tsNameIndex phaedra_i

-- -----------------------------------------------------------------------
-- Master table
-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_feature_value (
	well_id 			bigint not null,
	feature_id 			bigint not null,
	raw_numeric_value 	double precision,
	raw_string_value 	varchar(400),
	normalized_value 	double precision
)
TABLESPACE :tsNameData;

grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value to :accountNameWrite;
grant SELECT on phaedra.hca_feature_value to :accountNameRead;

-- -----------------------------------------------------------------------
-- Partition tables
-- -----------------------------------------------------------------------

create table phaedra.hca_feature_value_part_current () inherits (phaedra.hca_feature_value);

alter table phaedra.hca_feature_value_part_current
	add constraint well_id_check
	check (well_id > -1 and well_id < 9223372036854775807);

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

grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value_part_current to :accountNameWrite;
grant SELECT on phaedra.hca_feature_value_part_current to :accountNameRead;

-- -----------------------------------------------------------------------
-- Trigger: always insert into current partition
-- -----------------------------------------------------------------------

create or replace function phaedra.hca_feature_value_insert_trigger()
returns trigger as $$
BEGIN
    INSERT INTO phaedra.hca_feature_value_part_current VALUES (NEW.*);
    RETURN NULL;
END;
$$
language plpgsql;

CREATE TRIGGER hca_feature_value_insert_trigger
    BEFORE INSERT ON phaedra.hca_feature_value
    FOR EACH ROW EXECUTE PROCEDURE phaedra.hca_feature_value_insert_trigger();

-- -----------------------------------------------------------------------
-- Split partition function
-- -----------------------------------------------------------------------

create or replace function phaedra.hca_feature_value_split_partition(partName text, tsNameIndex text, accountNameRead text, accountNameWrite text)
returns void as $$
DECLARE
	wellIdCutoff bigint;
	oldWellIdCutoff bigint;
	newPartTable text;
BEGIN
	select max(phaedra.hca_feature_value_part_current.well_id) into wellIdCutoff from phaedra.hca_feature_value_part_current;
	select min(phaedra.hca_feature_value_part_current.well_id) - 1 into oldWellIdCutoff from phaedra.hca_feature_value_part_current;
	
	newPartTable := format('hca_feature_value_part_%s', partName);
	
	-- 1. Rename _part_current to _part_partName
	execute format('alter table phaedra.hca_feature_value_part_current rename to %s', newPartTable);
	execute format('alter table phaedra.%s rename constraint hca_feature_value_part_current_pk to %s_pk', newPartTable, newPartTable);
	execute format('alter table phaedra.%s rename constraint hca_feature_value_part_current_fk_f to %s_fk_f', newPartTable, newPartTable);
	execute format('alter table phaedra.%s rename constraint hca_feature_value_part_current_fk_w to %s_fk_w', newPartTable, newPartTable);
	
	-- 2. Modify the check constraint:
	execute format('alter table phaedra.%s drop constraint well_id_check, add constraint well_id_check check (well_id > %s and well_id <= %s)', newPartTable, oldWellIdCutoff, wellIdCutoff);

	-- 3. Create a new _part_current partition table using new well_id boundaries
	create table phaedra.hca_feature_value_part_current () inherits (phaedra.hca_feature_value);
	
	execute format('alter table phaedra.hca_feature_value_part_current add constraint well_id_check check (well_id > %s and well_id < 9223372036854775807)', wellIdCutoff);
	execute format('alter table phaedra.hca_feature_value_part_current add constraint hca_feature_value_part_current_pk primary key (well_id, feature_id) using index tablespace %s', tsNameIndex);
	
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
	
	execute format('grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value_part_current to "%s"', accountNameWrite);
	execute format('grant SELECT on phaedra.hca_feature_value_part_current to "%s"', accountNameRead);
END
$$
language plpgsql;