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

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_fk_w
	FOREIGN KEY (well_id)
	REFERENCES phaedra.hca_plate_well (well_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_fk_f
	FOREIGN KEY (feature_id)
	REFERENCES phaedra.hca_feature (feature_id)
	ON DELETE CASCADE;

grant INSERT, UPDATE, DELETE on phaedra.hca_feature_value to :accountNameWrite;
grant SELECT on phaedra.hca_feature_value to :accountNameRead;

-- -----------------------------------------------------------------------
-- Partition tables
-- -----------------------------------------------------------------------

create table phaedra.hca_feature_value_part_current () inherits (phaedra.hca_feature_value);

alter table phaedra.hca_feature_value_part_current
	add constraint hca_feature_value_part_current_pk
	primary key (well_id, feature_id)
	using index tablespace :tsNameIndex;
	
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
