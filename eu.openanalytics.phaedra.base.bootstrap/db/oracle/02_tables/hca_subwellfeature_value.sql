create type num_val_array as varray(10000) of binary_float;

create table hca_subwellfeature_value (
    well_id number not null,
    feature_id number not null,
    num_val num_val_array
) tablespace phaedra_d;

alter table hca_subwellfeature_value
	add constraint hca_swf_value_pk
	primary key (well_id, feature_id)
	using index tablespace phaedra_i;

alter table hca_subwellfeature_value
	add constraint hca_swf_value_fk_well
	foreign key (well_id)
	references phaedra.hca_plate_well(well_id)
	on delete cascade;

alter table hca_subwellfeature_value
	add constraint hca_swf_value_fk_ft
	foreign key (feature_id)
	references phaedra.hca_subwellfeature(subwellfeature_id)
	on delete cascade;

create or replace function array_length(ar in num_val_array, dim in number) return number is
begin
   return ar.count;
end;

create global temporary table hca_subwellfeature_value_tmp (
    well_id number not null,
    feature_id number not null,
    num_val num_val_array
)
on commit preserve rows;

grant select on hca_subwellfeature_value to phaedra_role_read;
grant insert, update, delete on hca_subwellfeature_value to phaedra_role_crud;
grant select on hca_subwellfeature_value_tmp to phaedra_role_read;
grant insert, update, delete on hca_subwellfeature_value_tmp to phaedra_role_crud;
grant execute on array_length to phaedra_role_read;
grant execute on num_val_array to phaedra_role_read;