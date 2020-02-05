create table hca_calculation_formula (
	formula_id 			number not null,
	formula_name		varchar2(300) not null,
	description			varchar2(300),
	category			varchar2(300),
	author				varchar2(300),
	formula				varchar2(4000),
	language			varchar2(300),
	scope				number,
	input_type			number
)
tablespace phaedra_d;

alter table hca_calculation_formula
	add constraint hca_calculation_formula_pk
	primary key ( formula_id )
	using index tablespace phaedra_i;

create sequence hca_calculation_formula_s
	increment by 1
	start with 1
	nomaxvalue
	nocycle
	nocache;
	
grant insert, update, delete on hca_calculation_formula to phaedra_role_crud;
grant select on hca_calculation_formula_s to phaedra_role_crud;
grant select on hca_calculation_formula to phaedra_role_read;