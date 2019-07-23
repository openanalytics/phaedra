
CREATE TABLE phaedra.hca_image_setting (
	image_setting_id bigint not null,
	zoom_ratio integer,
	gamma integer,
	pixel_size_x numeric,
	pixel_size_y numeric,
	pixel_size_z numeric
);

ALTER TABLE phaedra.hca_image_setting
	ADD CONSTRAINT hca_image_setting_pk
	PRIMARY KEY  (image_setting_id);

CREATE SEQUENCE phaedra.hca_image_setting_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_image_setting to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_image_setting to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_image_channel (
  image_channel_id bigint not null,
  channel_name varchar(100),
  description varchar(400),
  channel_type integer,
  channel_sequence integer,
  channel_source integer,
  color_mask integer,
  lookup_low integer,
  lookup_high integer,
  show_in_plate boolean,
  show_in_well boolean,
  alpha integer,
  level_min integer,
  level_max integer,
  bit_depth integer,
  image_setting_id bigint
);

ALTER TABLE phaedra.hca_image_channel
	ADD CONSTRAINT hca_image_channel_pk
	PRIMARY KEY  (image_channel_id);

CREATE SEQUENCE phaedra.hca_image_channel_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_image_channel to phaedra_role_crud;
GRANT SELECT ON phaedra. hca_image_channel to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_image_channel_config (
	image_channel_id		bigint not null,
	setting_name			varchar(100) not null,
	setting_value 			varchar(500)
);

ALTER TABLE phaedra.hca_image_channel_config
	ADD CONSTRAINT hca_image_channel_config_pk
	PRIMARY KEY (image_channel_id, setting_name);

ALTER TABLE phaedra.hca_image_channel_config
	ADD CONSTRAINT hca_image_channel_config_fk_1
	FOREIGN KEY (image_channel_id)
	REFERENCES phaedra.hca_image_channel(image_channel_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_image_channel_config to phaedra_role_crud;
GRANT SELECT ON phaedra. hca_image_channel_config to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_protocolclass (
	protocolclass_id			bigint not null,
	protocolclass_name			varchar(100),
	description					varchar(200),
	default_feature_id			bigint, 
	default_lims				varchar(25),
	default_layout_template		varchar(512),
	default_capture_config		varchar(512),
	is_editable					boolean default true,
	is_in_development			boolean default true,
	low_welltype				varchar(10), 
	high_welltype				varchar(10),
	image_setting_id			bigint,
	is_multi_dim_subwell_data 	boolean default false
);

ALTER TABLE phaedra.hca_protocolclass
	ADD CONSTRAINT hca_protocolclass_pk
	PRIMARY KEY  (protocolclass_id);

ALTER TABLE phaedra.hca_protocolclass
	ADD CONSTRAINT hca_pclass_fk_image_settings
	FOREIGN KEY (image_setting_id)
	REFERENCES phaedra.hca_image_setting(image_setting_id)
	ON DELETE SET NULL;
		
CREATE SEQUENCE phaedra.hca_protocolclass_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_protocolclass to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_protocolclass to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_protocol (
	protocol_id			bigint not null,
	protocol_name		varchar(100), 
	protocolclass_id	bigint,
	description			varchar(200),
	team_code			varchar(25) default 'NONE',
	upload_system		varchar(25) default 'NONE',
	image_setting_id	bigint
);

ALTER TABLE phaedra.hca_protocol
	ADD CONSTRAINT hca_protocol_pk
	PRIMARY KEY (protocol_id);

ALTER TABLE phaedra.hca_protocol
	ADD CONSTRAINT hca_protocol_fk_protocolclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_protocol
	ADD CONSTRAINT hca_prot_fk_image_settings
	FOREIGN KEY (image_setting_id)
	REFERENCES phaedra.hca_image_setting(image_setting_id)
	ON DELETE SET NULL;

CREATE INDEX hca_protocol_ix_01
	ON phaedra.hca_protocol (protocolclass_id);

CREATE SEQUENCE phaedra.hca_protocol_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_protocol to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_protocol to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_experiment (
	experiment_id		bigint not null, 
	experiment_name		varchar(100),
	experiment_dt		timestamp, 
	experiment_user		varchar(25),
	protocol_id			bigint, 
	description			varchar(200),
	comments			varchar(1600),
	multiplo_method		varchar(100),
	multiplo_parameter	varchar(100),
	archive_status		bigint default 0, 
	archive_user		varchar(25),
	archive_dt			timestamp
);

ALTER TABLE phaedra.hca_experiment
	ADD CONSTRAINT hca_experiment_pk
	PRIMARY KEY (experiment_id);

ALTER TABLE phaedra.hca_experiment
	ADD CONSTRAINT hca_experiment_fk_protocol
	FOREIGN KEY (protocol_id)
	REFERENCES phaedra.hca_protocol(protocol_id);

CREATE INDEX hca_experiment_ix_01
	ON phaedra.hca_experiment (experiment_dt);

CREATE INDEX hca_experiment_ix_02
	ON phaedra.hca_experiment (protocol_id);

CREATE SEQUENCE phaedra.hca_experiment_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_experiment to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_experiment to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_plate (
	plate_id			bigint not null,
	experiment_id		bigint not null, 
	sequence_in_run		integer not null,
	barcode 			varchar(200),
	barcode_source		varchar(25),
	description			varchar(200),
	plate_info 			varchar(100),
	link_status 		integer default 0, 
	link_user			varchar(25),
	link_dt				timestamp,
	calc_status			integer default 0,
	calc_error			varchar(50),
	calc_dt				timestamp, 
	validate_status		integer default 0, 
	validate_user		varchar(25),
	validate_dt			timestamp, 
	approve_status		integer default 0, 
	approve_user		varchar(25),
	approve_dt			timestamp, 
	upload_status		integer default 0, 
	upload_user			varchar(25),
	upload_dt			timestamp, 
	jpx_available		boolean default false,
	jpx_path			varchar(20), 
	celldata_available	boolean default false,
	data_xml			clob,
	plate_rows			integer,
	plate_columns 		integer
);

ALTER TABLE phaedra.hca_plate
	ADD CONSTRAINT hca_plate_pk
	PRIMARY KEY  ( plate_id );

ALTER TABLE phaedra.hca_plate
	ADD CONSTRAINT hca_plate_fk_experiment
	FOREIGN KEY (experiment_id)
	REFERENCES phaedra.hca_experiment(experiment_id);

CREATE INDEX hca_plate_ix_01
	ON phaedra.hca_plate (barcode);

CREATE INDEX hca_plate_ix_02
	ON phaedra.hca_plate (experiment_id);

CREATE SEQUENCE phaedra.hca_plate_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_plate to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_plate to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_welltype (
	welltype_code			varchar(10) not null,
	description				varchar(100)
);

ALTER TABLE phaedra.hca_welltype
	ADD CONSTRAINT hca_welltype_pk
	PRIMARY KEY (welltype_code);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_welltype to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_welltype to phaedra_role_read;

INSERT INTO phaedra.hca_welltype(welltype_code, description) VALUES ('EMPTY', 'Empty');
INSERT INTO phaedra.hca_welltype(welltype_code, description) VALUES ('LC', 'Low Control');
INSERT INTO phaedra.hca_welltype(welltype_code, description) VALUES ('HC', 'High Control');
INSERT INTO phaedra.hca_welltype(welltype_code, description) VALUES ('SAMPLE', 'Sample');

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_plate_compound (
	platecompound_id	bigint not null,			
	plate_id			bigint not null,
	compound_ty			varchar(10) not null,
	compound_nr			varchar(50) not null,
	validate_status		integer default 0, 
	validate_user		varchar(25),
	validate_dt			timestamp, 
	upload_status		integer default 0, 
	upload_user			varchar(25),
	upload_dt			timestamp,
	description			varchar(200),
	saltform			varchar(50)
);

ALTER TABLE phaedra.hca_plate_compound
	ADD CONSTRAINT hca_plate_compound_pk
	PRIMARY KEY (platecompound_id);

ALTER TABLE phaedra.hca_plate_compound
	ADD CONSTRAINT hca_plate_compound_fk_plate
	FOREIGN KEY (plate_id)
	REFERENCES phaedra.hca_plate(plate_id)
	ON DELETE CASCADE;

CREATE INDEX hca_plate_compound_ix_01
	ON phaedra.hca_plate_compound (compound_ty, compound_nr);
	
CREATE INDEX hca_plate_compound_ix_02
	ON phaedra.hca_plate_compound (plate_id);

CREATE UNIQUE INDEX hca_plate_compound_uix_01
	ON phaedra.hca_plate_compound (plate_id, compound_ty, compound_nr);

CREATE SEQUENCE phaedra.hca_plate_compound_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE; 	

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_plate_compound to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_plate_compound to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_plate_well (
	well_id					bigint not null,
	plate_id				bigint not null,
	row_nr					integer not null, 
	col_nr					integer not null,
	welltype_code			varchar(10),
	concentration			double precision, 
	is_valid				integer default 0,
	annotation_available 	boolean default false,
	platecompound_id		bigint,
	description				varchar(200)
);

ALTER TABLE phaedra.hca_plate_well
	ADD CONSTRAINT hca_plate_well_pk
	PRIMARY KEY (well_id);

ALTER TABLE phaedra.hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_plate
	FOREIGN KEY (plate_id)
	REFERENCES phaedra.hca_plate(plate_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_welltype
	FOREIGN KEY (welltype_code)
	REFERENCES phaedra.hca_welltype(welltype_code);

ALTER TABLE phaedra.hca_plate_well
	ADD CONSTRAINT hca_plate_well_fk_compound
	FOREIGN KEY (platecompound_id)
	REFERENCES phaedra.hca_plate_compound(platecompound_id);

CREATE INDEX hca_plate_well_ix_01 
	ON phaedra.hca_plate_well (welltype_code);

CREATE INDEX hca_plate_well_ix_02 
	ON phaedra.hca_plate_well (row_nr, col_nr);

CREATE INDEX hca_plate_well_ix_03 
	ON phaedra.hca_plate_well (plate_id);

CREATE INDEX hca_plate_well_ix_04 
	ON phaedra.hca_plate_well (platecompound_id);

CREATE SEQUENCE phaedra.hca_plate_well_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE; 

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_plate_well to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_plate_well to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_feature (
	feature_id				bigint not null,
	feature_name			varchar(100) not null,
	short_name				varchar(36),
	protocolclass_id		bigint, 
	is_numeric				boolean default false,
	is_logarithmic			boolean default false, 
	is_required				boolean default true,
	is_key					boolean default true,
	is_uploaded 			boolean default false,
	is_annotation 			boolean default false,
	is_classification_restricted boolean default false,
	calc_formula			varchar(2000),
	curve_normalization		varchar(25) default 'NONE',
	normalization_language	varchar(30),
	normalization_formula	varchar(2000),
	normalization_scope		integer,
	description 			varchar(250), 
	format_string			varchar(25),
	low_welltype			varchar(10), 
	high_welltype			varchar(10),
	calc_language			varchar(30),
	calc_trigger			varchar(30),
	calc_sequence			integer
);

-- -----------------------------------------------------------------------

ALTER TABLE phaedra.hca_feature
	ADD CONSTRAINT hca_feature_pk
	PRIMARY KEY (feature_id);

ALTER TABLE phaedra.hca_feature
	ADD CONSTRAINT hca_feature_fk_protocolclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE INDEX hca_feature_ix_01
	ON phaedra.hca_feature (protocolclass_id);

CREATE SEQUENCE phaedra.hca_feature_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_feature to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_feature to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_subwellfeature (
	subwellfeature_id			bigint not null,
	subwellfeature_name		varchar(100) not null,
	short_name				varchar(36),
	protocolclass_id		bigint, 
	is_numeric				boolean default false,
	is_logarithmic			boolean default false,
	is_key					boolean default true,
	calc_formula			varchar(1000),
	description 			varchar(250), 
	format_string			varchar(25),
	position_role			varchar(50)
);

ALTER TABLE phaedra.hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_pk
	PRIMARY KEY (subwellfeature_id);

ALTER TABLE phaedra.hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_fk_pc
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE INDEX hca_subwellfeature_ix_01
	ON phaedra.hca_subwellfeature (protocolclass_id);

CREATE SEQUENCE phaedra.hca_subwellfeature_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_subwellfeature to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_subwellfeature to phaedra_role_read;

-- ----------------------------------------------------------------------- 

CREATE TABLE phaedra.hca_feature_group (
	group_id				bigint not null,
	group_name				varchar(100) not null,
	description 			varchar(250),
	group_type				integer not null,
	protocolclass_id		bigint not null
);

ALTER TABLE phaedra.hca_feature_group
	ADD CONSTRAINT hca_feature_group_pk
	PRIMARY KEY (group_id);

ALTER TABLE phaedra.hca_feature_group
	ADD CONSTRAINT hca_fg_fk_protocolclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_feature
	ADD group_id bigint;
		
ALTER TABLE phaedra.hca_subwellfeature
 	ADD group_id bigint;
		
ALTER TABLE phaedra.hca_feature
	ADD CONSTRAINT hca_feature_fk_fg
	FOREIGN KEY (group_id)
	REFERENCES phaedra.hca_feature_group(group_id)
	ON DELETE SET NULL;

ALTER TABLE phaedra.hca_subwellfeature
	ADD CONSTRAINT hca_subwellfeature_fk_fg
	FOREIGN KEY (group_id)
	REFERENCES phaedra.hca_feature_group(group_id)
	ON DELETE SET NULL;

CREATE SEQUENCE phaedra.hca_feature_group_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_feature_group to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_feature_group to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_feature_value (
	well_id 			bigint not null,
	feature_id 			bigint not null,
	raw_numeric_value 	double precision,
	raw_string_value 	varchar(400),
	normalized_value 	double precision
);

ALTER TABLE phaedra.hca_feature_value
	ADD CONSTRAINT hca_feature_value_pk
	PRIMARY KEY (well_id, feature_id);

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

CREATE INDEX hca_feature_value_ix1
	ON phaedra.hca_feature_value(feature_id);
	
GRANT INSERT, UPDATE, DELETE ON phaedra.hca_feature_value to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_feature_value to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_curve_setting (
	feature_id				bigint not null,
	setting_name			varchar(100)  not null,
	setting_value 			varchar(250)
);

ALTER TABLE phaedra.hca_curve_setting
	ADD CONSTRAINT hca_curve_setting_pk
	PRIMARY KEY  ( feature_id, setting_name );

ALTER TABLE phaedra.hca_curve_setting
	ADD CONSTRAINT hca_curve_setting_fk_feature
	FOREIGN KEY (feature_id)
	REFERENCES phaedra.hca_feature(feature_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_curve_setting to phaedra_role_crud;
GRANT SELECT ON phaedra. hca_curve_setting to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_curve (
	curve_id bigint not null,
	feature_id bigint not null,
	model_id varchar(50) not null,
	group_by_1 varchar(100),
	group_by_2 varchar(100),
	group_by_3 varchar(100),
	fit_date timestamp not null,
	fit_version varchar(100),
	error_code bigint not null,
	plot bytea
);

ALTER TABLE phaedra.hca_curve
	ADD CONSTRAINT hca_curve_pk
	PRIMARY KEY  ( curve_id );
    
ALTER TABLE phaedra.hca_curve
	ADD CONSTRAINT hca_curve_fk_feature
	FOREIGN KEY (feature_id)
	REFERENCES phaedra.hca_feature(feature_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_curve_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;
	
CREATE INDEX hca_curve_ix_1
	ON phaedra.hca_curve(feature_id);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_curve to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_curve to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_curve_property (
	curve_id 			bigint not null,
	property_name		varchar(100) not null,
	numeric_value 		double precision,
	string_value		varchar(150),
	binary_value		bytea
);

ALTER TABLE phaedra.hca_curve_property
	ADD CONSTRAINT hca_curve_property_pk
	PRIMARY KEY  ( curve_id, property_name );

ALTER TABLE phaedra.hca_curve_property
	ADD CONSTRAINT hca_curve_property_fk_curve
	FOREIGN KEY (curve_id)
	REFERENCES phaedra.hca_curve(curve_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_curve_property to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_curve_property to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_curve_setting_custom (
	curve_id				bigint not null,
	setting_name			varchar(100)  not null,
	setting_value 			varchar(250)
);

ALTER TABLE phaedra.hca_curve_setting_custom
	ADD CONSTRAINT hca_curve_setting_custom_pk
	PRIMARY KEY  ( curve_id, setting_name );

ALTER TABLE phaedra.hca_curve_setting_custom
	ADD CONSTRAINT hca_c_s_custom_fk_curve
	FOREIGN KEY (curve_id)
	REFERENCES phaedra.hca_curve(curve_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_curve_setting_custom to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_curve_setting_custom to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_curve_compound (
	curve_id bigint not null,
  	platecompound_id bigint not null
);

ALTER TABLE phaedra.hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_pk
	PRIMARY KEY  ( curve_id, platecompound_id );

ALTER TABLE phaedra.hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_fk_curve
	FOREIGN KEY (curve_id)
	REFERENCES phaedra.hca_curve(curve_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_curve_compound
	ADD CONSTRAINT hca_curve_compound_fk_comp
	FOREIGN KEY (platecompound_id)
	REFERENCES phaedra.hca_plate_compound(platecompound_id)
	ON DELETE CASCADE;

CREATE INDEX hca_curve_compound_ix_1
	ON phaedra.hca_curve_compound(platecompound_id);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_curve_compound to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_curve_compound to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_colormethod_setting (
	feature_id				bigint not null,
	setting_name			varchar(100) not null,
	setting_value 			varchar(250)
);

ALTER TABLE phaedra.hca_colormethod_setting
	ADD CONSTRAINT hca_colormethod_setting_pk
	PRIMARY KEY  ( feature_id, setting_name );

ALTER TABLE phaedra.hca_colormethod_setting
	ADD CONSTRAINT hca_cm_setting_fk_feature
	FOREIGN KEY (feature_id)
	REFERENCES phaedra.hca_feature(feature_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_colormethod_setting to phaedra_role_crud;
GRANT SELECT ON phaedra. hca_colormethod_setting to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_user (
	user_code			VARCHAR(25) not null, 
	email				VARCHAR(50),
	last_logon			TIMESTAMP
);

ALTER TABLE phaedra.hca_user
	ADD CONSTRAINT hca_user_pk
	PRIMARY KEY  ( user_code );

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_user to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_user to phaedra_role_read;

-- ----------------------------------------------------------------------- 

CREATE TABLE phaedra.hca_user_session (
	session_id			bigint not null,
	user_code			VARCHAR(25) not null,
	login_date			TIMESTAMP not null,
	host				VARCHAR(50),
	version				VARCHAR(50)
);

ALTER TABLE phaedra.hca_user_session
	ADD CONSTRAINT hca_user_session_pk
	PRIMARY KEY  ( session_id );

CREATE SEQUENCE phaedra.hca_user_session_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_user_session to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_user_session to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_preference (
	pref_type			varchar(25) not null,
	pref_user			varchar(25) not null,
	pref_item			varchar(200) not null,
	pref_value			text
);

ALTER TABLE phaedra.hca_preference
	ADD CONSTRAINT hca_preference_pk
	PRIMARY KEY  ( pref_type, pref_user, pref_item );

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_preference to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_preference to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_reading (
	reading_id				bigint not null,
	reading_dt				timestamp,
	reading_user			varchar(50),
	file_name				varchar(100),
	file_part				integer,
	file_info				varchar(500),
	barcode					varchar(64),
	plate_rows				integer,
	plate_columns			integer,
	src_path				varchar(300),
	capture_path			varchar(300),
	instrument				varchar(150),
	protocol				varchar(100),
	experiment				varchar(100),
	link_dt					timestamp,
	link_user				varchar(50),
	link_status				integer default 0
);

ALTER TABLE phaedra.hca_reading
	ADD CONSTRAINT hca_reading_pk
	PRIMARY KEY  ( reading_id );

CREATE SEQUENCE phaedra.hca_reading_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_reading to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_reading to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_classification (
	classification_id 	bigint not null,
	pattern 			varchar(100),
	pattern_type 		varchar(30),
	description 		varchar(300),
	rgb_color 			integer,
	label 				varchar(100),
	symbol				varchar(50),
	subwellfeature_id 		bigint,
	wellfeature_id 		bigint
);

ALTER TABLE phaedra.hca_classification
	ADD CONSTRAINT hca_classification_pk
	PRIMARY KEY  ( classification_id );
    
ALTER TABLE phaedra.hca_classification
	ADD CONSTRAINT hca_cf_fk_subwell_feature
	FOREIGN KEY (subwellfeature_id)
	REFERENCES phaedra.hca_subwellfeature(subwellfeature_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_classification_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_classification to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_classification to phaedra_role_read;

-- ----------------------------------------------------------------------- 

CREATE TABLE phaedra.hca_object_log_eventtype (
	event_code			VARCHAR(10) not null,
	event_label			VARCHAR(25) not null,
	event_description	VARCHAR(200)
);

ALTER TABLE phaedra.hca_object_log_eventtype
	ADD CONSTRAINT hca_object_log_eventtype_pk
	PRIMARY KEY  ( event_code );

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_object_log_eventtype to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_object_log_eventtype to phaedra_role_read;

INSERT INTO phaedra.hca_object_log_eventtype(event_code, event_label, event_description) VALUES ('CHANGED','Object Changed','A field or property of the object has been changed.');
INSERT INTO phaedra.hca_object_log_eventtype(event_code, event_label, event_description) VALUES ('REMOVED','Object Removed','The object has been removed.');

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_object_log (
	log_id				bigint not null,
	timestamp			timestamp not null,
	user_code			VARCHAR(25) not null,
	event_code			VARCHAR(10) not null,
	object_class		VARCHAR(50) not null,
	object_id			bigint not null,
	object_prop_1		VARCHAR(50),
	object_prop_2		VARCHAR(50),
	old_value			VARCHAR(200),
	new_value			VARCHAR(200),
	remark				VARCHAR(200)
);

ALTER TABLE phaedra.hca_object_log
	ADD CONSTRAINT hca_object_log_pk
	PRIMARY KEY  ( log_id );

ALTER TABLE phaedra.hca_object_log
	ADD CONSTRAINT hca_object_log_fk_eventtype
	FOREIGN KEY (event_code)
	REFERENCES phaedra.hca_object_log_eventtype(event_code);

CREATE INDEX hca_object_log_ix_1 ON phaedra.hca_object_log(object_class);
CREATE INDEX hca_object_log_ix_2 ON phaedra.hca_object_log(object_id);

CREATE SEQUENCE phaedra.hca_object_log_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_object_log to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_object_log to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_plate_template (
	template_id			bigint not null,
	protocolclass_id	bigint not null,
	template_name		varchar2(1000) not null,
	rows				integer not null,
	columns				integer not null,
	creator				varchar2(200),
	data_xml			clob
);

ALTER TABLE phaedra.hca_plate_template
	ADD CONSTRAINT hca_plate_template_pk
	PRIMARY KEY  ( template_id );

ALTER TABLE phaedra.hca_plate_template
	ADD CONSTRAINT hca_plate_template_fk_protocolclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_plate_template_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_plate_template to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_plate_template to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_upload (
	platecompound_id	bigint, 
	compound_ty			varchar(10),
	compound_nr			varchar(25),
	protocol_id			bigint, 
	protocol_name		varchar(100),
	experiment_id		bigint, 
	experiment_name		varchar(100),
	experiment_dt		timestamp, 
	experiment_user		varchar(25), 
	plate_id			bigint,
	plate_barcode		varchar(64),
	plate_description	varchar(200), 
	plate_info			varchar(100),
	data_xml			clob,
	upload_system		varchar(25)
);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_upload to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_upload to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_upload_result (
	platecompound_id		bigint, 
	curve_id				bigint,
	feature_id				bigint, 
	feature_name			varchar(100), 
	result_type				varchar(25), 
	qualifier				varchar(25),
	value					varchar(100)
);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_upload_result to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_upload_result to phaedra_role_read;

-- -----------------------------------------------------------------------
 
CREATE TABLE phaedra.hca_upload_point (
	well_id					bigint, 
	platecompound_id		bigint,
	curve_id				bigint,
	feature_id				bigint, 
	feature_name			varchar(100), 
	concentration			double precision, 
	is_valid				boolean, 
	value					double precision, 
	normalized				double precision
);

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_upload_point to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_upload_point to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_mail_distribution_list (
	list_id					bigint not null,
	list_name				varchar(100),
	label					varchar(200)
);

ALTER TABLE phaedra.hca_mail_distribution_list
	ADD CONSTRAINT hca_mail_distribution_list_pk
	PRIMARY KEY  ( list_id );

ALTER TABLE phaedra.hca_mail_distribution_list
	ADD CONSTRAINT hca_mail_distribution_list_unq
	UNIQUE ( list_name );
		
CREATE SEQUENCE phaedra.hca_mail_distribution_list_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_mail_distribution_list to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_mail_distribution_list to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_mail_list_member (
	email_address			varchar(200) not null,
	list_id					bigint not null
);

ALTER TABLE phaedra.hca_mail_list_member
	ADD CONSTRAINT hca_mail_list_member_pk
	PRIMARY KEY  ( list_id, email_address );
	
GRANT INSERT, UPDATE, DELETE ON phaedra.hca_mail_list_member to phaedra_role_crud;
GRANT SELECT ON phaedra. hca_mail_list_member to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_dc_log (
	log_id					bigint not null,
	log_date				timestamp not null,
	log_source				varchar(100),
	status_code				integer default 0,
	message					varchar(1000),
	error					varchar(2000),
	source_path				varchar(200),
	source_identifier		varchar(200),
	reading					varchar(200),
	task_id					varchar(50),
	task_user				varchar(50)
);

ALTER TABLE phaedra.hca_dc_log
	ADD CONSTRAINT hca_dc_log_pk
	PRIMARY KEY  ( log_id );
	
CREATE SEQUENCE phaedra.hca_dc_log_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_dc_log to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_dc_log to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_dc_scan_job (
	job_id					bigint not null, 
	schedule				varchar(100),
	scanner_type			varchar(200),
	label					varchar(100),
	description				varchar(1000),
	config					clob 
);

ALTER TABLE phaedra.hca_dc_scan_job
	ADD CONSTRAINT hca_dc_scan_job_pk
	PRIMARY KEY  ( job_id );
	
CREATE SEQUENCE phaedra.hca_dc_scan_job_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_dc_scan_job to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_dc_scan_job to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_dc_metric (
	metric_id				bigint not null,
	timestamp				timestamp not null,
	disk_usage				bigint,
	ram_usage				bigint,
	cpu_usage				double precision,
	dl_speed				bigint,
	ul_speed				bigint
);

ALTER TABLE phaedra.hca_dc_metric
	ADD CONSTRAINT hca_dc_metric_pk
	PRIMARY KEY  ( metric_id );
	
CREATE SEQUENCE phaedra.hca_dc_metric_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_dc_metric to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_dc_metric to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_dc_watched_folder (
	folder_id				bigint not null, 
	location				varchar(1000),
	capture_config			varchar(200),
	pattern					varchar(200),
	protocolId				bigint
);

ALTER TABLE phaedra.hca_dc_watched_folder
	ADD CONSTRAINT hca_dc_watched_folder_pk
	PRIMARY KEY  ( folder_id );
	
CREATE SEQUENCE phaedra.hca_dc_watched_folder_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_dc_watched_folder to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_dc_watched_folder to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_silo_group (
	group_id				bigint not null,
	group_name				varchar(100), 
	description				varchar(200),
	protocolclass_id		bigint not null,
	owner					varchar(25),
	creation_date			timestamp,
	group_type				integer not null,
	access_scope			varchar(25),
	is_example 				boolean default false
);

ALTER TABLE phaedra.hca_silo_group
	ADD CONSTRAINT hca_silo_group_pk
	PRIMARY KEY  ( group_id );

ALTER TABLE phaedra.hca_silo_group
	ADD CONSTRAINT hca_silo_group_fk_pclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_silo_group_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_silo_group to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_silo_group to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_silo (
	silo_id					bigint not null,
	silo_name				varchar(100), 
	description				varchar(200),
	protocolclass_id		bigint not null,
	owner					varchar(25),
	creation_date			timestamp,
	silo_type				integer not null,
	access_scope			varchar(25),
	is_example 				boolean default false
);

ALTER TABLE phaedra.hca_silo
	ADD CONSTRAINT hca_silo_pk
	PRIMARY KEY  ( silo_id );

ALTER TABLE phaedra.hca_silo
	ADD CONSTRAINT hca_silo_fk_protocolclass
	FOREIGN KEY (protocolclass_id)
	REFERENCES phaedra.hca_protocolclass(protocolclass_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_silo_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_silo to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_silo to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_silo_group_member (
	silo_id			bigint not null,
	group_id		bigint not null
);

ALTER TABLE phaedra.hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_pk
	PRIMARY KEY  ( silo_id, group_id );

ALTER TABLE phaedra.hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_fk_silo
	FOREIGN KEY (silo_id)
	REFERENCES phaedra.hca_silo(silo_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_silo_group_member
	ADD CONSTRAINT hca_silo_group_member_fk_group
	FOREIGN KEY (group_id)
	REFERENCES phaedra.hca_silo_group(group_id)
	ON DELETE CASCADE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_silo_group_member to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_silo_group_member to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.query (
	query_id				bigint not null,
	query_name				varchar(100),
	description				varchar(255),
	remark					varchar(255),
	query_user				varchar(25),
	query_dt				timestamp, 
	is_public				boolean default false,
	example					boolean default false,
	type					varchar(255),
	max_results_set			boolean default true,
	max_results				integer
);

ALTER TABLE phaedra.query
	ADD CONSTRAINT query_pk
	PRIMARY KEY  ( query_id );

CREATE SEQUENCE phaedra.query_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.query to phaedra_role_crud;
GRANT SELECT ON phaedra.query to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.query_filter (
	query_filter_id		bigint not null,
	query_id			bigint not null,
	type				varchar(255),
	column_name			varchar(100),
	positive			boolean default true,
	operator_type		varchar(25),
	operator			varchar(25),
	case_sensitive		boolean default false,
	value				bytea	
);

ALTER TABLE phaedra.query_filter
	ADD CONSTRAINT query_filter_pk
	PRIMARY KEY  ( query_filter_id );

ALTER TABLE phaedra.query_filter
	ADD CONSTRAINT query_filter_fk_query
	FOREIGN KEY (query_id)
	REFERENCES phaedra.query(query_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.query_filter_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.query_filter to phaedra_role_crud;
GRANT SELECT ON phaedra.query_filter to phaedra_role_read;

-- ----------------------------------------------------------------------- 

CREATE TABLE phaedra.query_ordering (
	query_ordering_id	bigint not null,
	query_id			bigint not null,
	column_name			varchar(100),
	column_type			varchar(25),
	ascending			boolean default true,	
	case_sensitive		boolean default false,
	ordering_index		integer not null
);

ALTER TABLE phaedra.query_ordering
	ADD CONSTRAINT query_ordering_pk
	PRIMARY KEY  ( query_ordering_id );

ALTER TABLE phaedra.query_ordering
	ADD CONSTRAINT query_ordering_fk_query
	FOREIGN KEY (query_id)
	REFERENCES phaedra.query(query_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.query_ordering_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.query_ordering to phaedra_role_crud;
GRANT SELECT ON phaedra.query_ordering to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.HCA_PART_SETTINGS (
    SETTINGS_ID    	bigint NOT NULL,
    PROTOCOL_ID 	bigint NOT NULL,
    USER_CODE   	varchar(20) NOT NULL,
    CLASS_NAME	 	varchar(256) NOT NULL,
    NAME		   	varchar(100) NOT NULL,
	IS_GLOBAL		bigint DEFAULT 0,
	IS_TEMPLATE		bigint DEFAULT 0,
    PROPERTIES 		text
);

ALTER TABLE phaedra.HCA_PART_SETTINGS 
	ADD CONSTRAINT HCA_PART_SETTINGS_PK
	PRIMARY KEY (SETTINGS_ID);
	
ALTER TABLE phaedra.HCA_PART_SETTINGS
	ADD CONSTRAINT FK_HCA_PART_SETTINGS_PROTOCOL
	FOREIGN KEY (PROTOCOL_ID)
	REFERENCES phaedra.HCA_PROTOCOL (PROTOCOL_ID)
	ON DELETE CASCADE;
	
CREATE SEQUENCE phaedra.HCA_PART_SETTINGS_S
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE on phaedra.HCA_PART_SETTINGS to phaedra_role_crud;
GRANT SELECT ON phaedra.HCA_PART_SETTINGS to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.HCA_REPORT (
    REPORT_ID    		bigint NOT NULL,
    PROTOCOL_ID 		bigint NOT NULL,
    USER_CODE   		VARCHAR(20) NOT NULL,
    NAME		   		VARCHAR(100) NOT NULL,
	DESCRIPTION			VARCHAR(300),
	IS_GLOBAL			BOOLEAN DEFAULT FALSE,
	IS_TEMPLATE			BOOLEAN DEFAULT FALSE,
	TEMPLATE_STYLE		bigint DEFAULT 0,
    PAGE_SIZE			VARCHAR(6) NOT NULL,
	PAGE_ORIENTATION	VARCHAR(12) NOT NULL
);

ALTER TABLE phaedra.HCA_REPORT
	ADD CONSTRAINT HCA_REPORT_PK
	PRIMARY KEY (REPORT_ID);
	
ALTER TABLE phaedra.HCA_REPORT
	ADD CONSTRAINT FK_HCA_REPORT_PROTOCOL
	FOREIGN KEY (PROTOCOL_ID)
	REFERENCES phaedra.HCA_PROTOCOL (PROTOCOL_ID)
	ON DELETE CASCADE;
	
CREATE SEQUENCE phaedra.HCA_REPORT_S
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE on phaedra.HCA_REPORT to phaedra_role_crud;
GRANT SELECT ON phaedra.HCA_REPORT to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.HCA_REPORT_PAGE (
    REPORT_PAGE_ID 		bigint NOT NULL,
    REPORT_ID			bigint NOT NULL,
    PART_SETTINGS_ID	bigint,
	PAGE_INPUT			text,
	PAGE_STYLE			bigint DEFAULT 0,
	PAGE_ORDER			bigint NOT NULL,
	PAGE_GROUP			VARCHAR(100),
	TITLE				VARCHAR(100),
	DESCRIPTION			VARCHAR(200),
	CLASS_NAME			VARCHAR(256)
);

ALTER TABLE phaedra.HCA_REPORT_PAGE
	ADD CONSTRAINT HCA_REPORT_PAGE_PK
	PRIMARY KEY (REPORT_PAGE_ID);
	
ALTER TABLE phaedra.HCA_REPORT_PAGE
	ADD CONSTRAINT FK_HCA_REPORT_PAGE_REPORT
	FOREIGN KEY (REPORT_ID)
	REFERENCES phaedra.HCA_REPORT (REPORT_ID)
	ON DELETE CASCADE;
	
ALTER TABLE phaedra.HCA_REPORT_PAGE
	ADD CONSTRAINT FK_HCA_REP_PAGE_PART_SETTINGS
	FOREIGN KEY (PART_SETTINGS_ID)
	REFERENCES phaedra.HCA_PART_SETTINGS (SETTINGS_ID)
	ON DELETE CASCADE;
	
CREATE SEQUENCE phaedra.HCA_REPORT_PAGE_S
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE on phaedra.HCA_REPORT_PAGE to phaedra_role_crud;
GRANT SELECT ON phaedra.HCA_REPORT_PAGE to phaedra_role_read;

-- -----------------------------------------------------------------------

CREATE TABLE phaedra.hca_psp (
	psp_id				bigint not null,
	psp_name 			varchar(100),
	workbench_state		text,
	owner 				varchar(50),
	access_scope 		varchar(50),
	feature_id			bigint
);

ALTER TABLE phaedra.hca_psp
	ADD CONSTRAINT hca_psp_pk
	PRIMARY KEY (psp_id);

ALTER TABLE phaedra.hca_psp
	ADD CONSTRAINT hca_psp_fk_feature
	FOREIGN KEY (feature_id)
	REFERENCES hca_feature(feature_id);

CREATE SEQUENCE phaedra.hca_psp_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_psp to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_psp to phaedra_role_read;

-- -----------------------------------------------------------------------

create table phaedra.hca_psp_part_ref (
	part_ref_id			bigint not null,
	psp_id				bigint not null,
	part_id 			varchar(100) not null,
	part_secondary_id	varchar(100),
	part_settings_id	bigint
);

ALTER TABLE phaedra.hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_pk
	PRIMARY KEY (part_ref_id);

ALTER TABLE phaedra.hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_fk_psp
	FOREIGN KEY (psp_id)
	REFERENCES hca_psp(psp_id)
	ON DELETE CASCADE;

ALTER TABLE phaedra.hca_psp_part_ref
	ADD CONSTRAINT hca_psp_part_ref_fk_part_sett
	FOREIGN KEY (part_settings_id)
	REFERENCES hca_part_settings(settings_id)
	ON DELETE CASCADE;

CREATE SEQUENCE phaedra.hca_psp_part_ref_s
	INCREMENT BY 1
	START WITH 1
	MAXVALUE 9223372036854775807
	NO CYCLE;

GRANT INSERT, UPDATE, DELETE ON phaedra.hca_psp_part_ref to phaedra_role_crud;
GRANT SELECT ON phaedra.hca_psp_part_ref to phaedra_role_read;

-- Project -------------------------------------------------------------------

create table phaedra.hca_project (
		project_id			bigint not null,
		name				varchar(100) not null,
		description			varchar(1000),
		owner				varchar(25),
		team_code			varchar(25) default 'NONE',
		access_scope		varchar(25)
	);

alter table phaedra.hca_project
	add constraint hca_project_pk
	primary key (project_id);

create sequence phaedra.hca_project_s
	increment by 1
	start with 1
	maxvalue 9223372036854775807
	no cycle;

grant insert, update, delete on phaedra.hca_project to phaedra_role_crud;
grant select on phaedra.hca_project to phaedra_role_read;

-- ---------------------------------------------------------------------------

create table phaedra.hca_project_experiment (
		project_id			bigint not null,
		experiment_id		bigint not null
	);

alter table phaedra.hca_project_experiment
	add constraint hca_project_experiment_pk
	primary key (project_id, experiment_id);

alter table phaedra.hca_project_experiment
	add constraint hca_project_experiment_fk_project
	foreign key (project_id)
	references phaedra.hca_project(project_id)
	on delete cascade;

alter table phaedra.hca_project_experiment
	add constraint hca_project_experiment_fk_experiment
	foreign key (experiment_id)
	references phaedra.hca_experiment(experiment_id)
	on delete cascade;

grant insert, update, delete on phaedra.hca_project_experiment to phaedra_role_crud;
grant select on phaedra.hca_project_experiment to phaedra_role_read;
