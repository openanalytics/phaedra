create table phaedra.hca_object_property (
	object_class		varchar(200) not null,
	object_id 			number not null,
	property_name		varchar(200) not null,
	numeric_value 		binary_double,
	string_value		varchar(1000),
	binary_value		blob
) tablespace phaedra_d;

alter table phaedra.hca_object_property
	add constraint hca_object_property_pk
	primary key ( object_class, object_id, property_name )
	using index tablespace phaedra_i;

grant insert, update, delete on phaedra.hca_object_property to phaedra_role_crud;
grant select on phaedra.hca_object_property to phaedra_role_read;
