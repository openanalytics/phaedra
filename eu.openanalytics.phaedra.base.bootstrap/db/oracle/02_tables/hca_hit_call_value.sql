create table hca_hit_call_value (
	well_id 			number not null,
	feature_id 			number not null,
	hit_value 			binary_double
)
tablespace phaedra_d;

alter table hca_hit_call_value
	add constraint hca_hit_call_value_pk
	primary key (well_id, feature_id)
	using index tablespace phaedra_i;

alter table hca_hit_call_value
	add constraint hca_hit_call_value_fk_w
	foreign key (well_id)
	references hca_plate_well (well_id)
	on delete cascade;

alter table hca_hit_call_value
	add constraint hca_hit_call_value_fk_f
	foreign key (feature_id)
	references hca_feature (feature_id)
	on delete cascade;

create index hca_hit_call_value_ix1
	on hca_hit_call_value(feature_id)
	online nologging tablespace phaedra_i;
	
grant insert, update, delete on hca_hit_call_value to phaedra_role_crud;
grant select on hca_hit_call_value to phaedra_role_read;