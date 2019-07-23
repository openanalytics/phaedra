create table hca_project_experiment (
		project_id			bigint not null,
		experiment_id		bigint not null
	)
	tablespace phaedra_d;

alter table hca_project_experiment
	add constraint hca_project_experiment_pk
	primary key (project_id, experiment_id)
	using index tablespace phaedra_i;

alter table hca_project_experiment
	add constraint hca_project_experiment_fk_project
	foreign key (project_id)
	references hca_project(project_id)
	on delete cascade;

alter table phaedra.hca_project_experiment
	add constraint hca_project_experiment_fk_experiment
	foreign key (experiment_id)
	references hca_experiment(experiment_id)
	on delete cascade;

grant insert, update, delete on hca_project_experiment to phaedra_role_crud;
grant select on hca_project_experiment to phaedra_role_read;
