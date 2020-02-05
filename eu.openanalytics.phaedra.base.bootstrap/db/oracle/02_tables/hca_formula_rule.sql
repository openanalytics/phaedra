create table hca_formula_rule (
	rule_id 			number not null,
	rule_name			varchar2(300) not null,
	formula_id			number not null,
	ruleset_id			number not null,
	ruleset_sequence	number not null,
	threshold 			binary_double
)
tablespace phaedra_d;

alter table hca_formula_rule
	add constraint hca_formula_rule_pk
	primary key ( rule_id )
	using index tablespace phaedra_i;

alter table hca_formula_rule
	add constraint hca_formula_rule_fk_formula
	foreign key (formula_id)
	references hca_calculation_formula(formula_id)
	on delete cascade;
	
alter table hca_formula_rule
	add constraint hca_formula_rule_fk_ruleset
	foreign key (ruleset_id)
	references hca_formula_ruleset(ruleset_id)
	on delete cascade;
	
create sequence hca_formula_rule_s
	increment by 1
	start with 1
	nomaxvalue
	nocycle
	nocache;
	
grant insert, update, delete on hca_formula_rule to phaedra_role_crud;
grant select on hca_formula_rule_s to phaedra_role_crud;
grant select on hca_formula_rule to phaedra_role_read;
