create table hca_project (
		project_id			bigint not null,
		name				varchar2(100) not null,
		description			varchar2(1000),
		owner				varchar2(25),
		team_code			varchar2(25) default 'NONE',
		access_scope		varchar2(25)
	)
	tablespace phaedra_d;

alter table hca_project
	add constraint hca_project_pk
	primary key (project_id)
	using index tablespace phaedra_i;

create sequence hca_project_s
	increment by 1
	start with 1
	nomaxvalue
	nocycle
	nocache;

grant insert, update, delete on hca_project to phaedra_role_crud;
grant select on hca_project_s to phaedra_role_crud;
grant select on hca_project to phaedra_role_read;
