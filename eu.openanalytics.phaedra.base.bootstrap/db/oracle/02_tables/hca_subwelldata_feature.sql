create table phaedra.hca_subwelldata_feature (
	protocolclass_id number not null,
	feature_id number not null,
	sequence_nr number
);

alter table phaedra.hca_subwelldata_feature
	add constraint hca_subwelldata_feature_pk
	primary key (protocolclass_id, feature_id);
	
grant INSERT, UPDATE, DELETE on phaedra.hca_subwelldata_feature to phaedra_role_crud;
grant SELECT on phaedra.hca_subwelldata_feature to phaedra_role_read;