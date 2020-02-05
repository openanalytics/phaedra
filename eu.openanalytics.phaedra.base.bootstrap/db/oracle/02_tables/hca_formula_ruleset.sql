create table hca_formula_ruleset (
	ruleset_id 			number not null,
	feature_id			number not null,
	type				number not null,
	show_in_ui			number default 1,
	color				number,
	style				number
)
tablespace phaedra_d;

alter table hca_formula_ruleset
	add constraint hca_formula_ruleset_pk
	primary key ( ruleset_id )
	using index tablespace phaedra_i;

alter table hca_formula_ruleset
	add constraint hca_formula_ruleset_fk_feature
	foreign key (feature_id)
	references hca_feature(feature_id)
	on delete cascade;
	
create sequence hca_formula_ruleset_s
	increment by 1
	start with 1
	nomaxvalue
	nocycle
	nocache;
	
grant insert, update, delete on hca_formula_ruleset to phaedra_role_crud;
grant select on hca_formula_ruleset_s to phaedra_role_crud;
grant select on hca_formula_ruleset to phaedra_role_read;
