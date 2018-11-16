
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
	barcode 			varchar(64),
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

create table phaedra.hca_subwelldata (
	well_id bigint not null,
	cell_id bigint not null,
	f0_num_val float,f1_num_val float,f2_num_val float,f3_num_val float,f4_num_val float,f5_num_val float,f6_num_val float,f7_num_val float,f8_num_val float,f9_num_val float,f10_num_val float,f11_num_val float,f12_num_val float,f13_num_val float,f14_num_val float,f15_num_val float,f16_num_val float,f17_num_val float,f18_num_val float,f19_num_val float,f20_num_val float,f21_num_val float,f22_num_val float,f23_num_val float,f24_num_val float,f25_num_val float,f26_num_val float,f27_num_val float,f28_num_val float,f29_num_val float,f30_num_val float,f31_num_val float,f32_num_val float,f33_num_val float,f34_num_val float,f35_num_val float,f36_num_val float,f37_num_val float,f38_num_val float,f39_num_val float,f40_num_val float,f41_num_val float,f42_num_val float,f43_num_val float,f44_num_val float,f45_num_val float,f46_num_val float,f47_num_val float,f48_num_val float,f49_num_val float,f50_num_val float,f51_num_val float,f52_num_val float,f53_num_val float,f54_num_val float,f55_num_val float,f56_num_val float,f57_num_val float,f58_num_val float,f59_num_val float,f60_num_val float,f61_num_val float,f62_num_val float,f63_num_val float,f64_num_val float,f65_num_val float,f66_num_val float,f67_num_val float,f68_num_val float,f69_num_val float,f70_num_val float,f71_num_val float,f72_num_val float,f73_num_val float,f74_num_val float,f75_num_val float,f76_num_val float,f77_num_val float,f78_num_val float,f79_num_val float,f80_num_val float,f81_num_val float,f82_num_val float,f83_num_val float,f84_num_val float,f85_num_val float,f86_num_val float,f87_num_val float,f88_num_val float,f89_num_val float,f90_num_val float,f91_num_val float,f92_num_val float,f93_num_val float,f94_num_val float,f95_num_val float,f96_num_val float,f97_num_val float,f98_num_val float,f99_num_val float,f100_num_val float,f101_num_val float,f102_num_val float,f103_num_val float,f104_num_val float,f105_num_val float,f106_num_val float,f107_num_val float,f108_num_val float,f109_num_val float,f110_num_val float,f111_num_val float,f112_num_val float,f113_num_val float,f114_num_val float,f115_num_val float,f116_num_val float,f117_num_val float,f118_num_val float,f119_num_val float,f120_num_val float,f121_num_val float,f122_num_val float,f123_num_val float,f124_num_val float,f125_num_val float,f126_num_val float,f127_num_val float,f128_num_val float,f129_num_val float,f130_num_val float,f131_num_val float,f132_num_val float,f133_num_val float,f134_num_val float,f135_num_val float,f136_num_val float,f137_num_val float,f138_num_val float,f139_num_val float,f140_num_val float,f141_num_val float,f142_num_val float,f143_num_val float,f144_num_val float,f145_num_val float,f146_num_val float,f147_num_val float,f148_num_val float,f149_num_val float,f150_num_val float,f151_num_val float,f152_num_val float,f153_num_val float,f154_num_val float,f155_num_val float,f156_num_val float,f157_num_val float,f158_num_val float,f159_num_val float,f160_num_val float,f161_num_val float,f162_num_val float,f163_num_val float,f164_num_val float,f165_num_val float,f166_num_val float,f167_num_val float,f168_num_val float,f169_num_val float,f170_num_val float,f171_num_val float,f172_num_val float,f173_num_val float,f174_num_val float,f175_num_val float,f176_num_val float,f177_num_val float,f178_num_val float,f179_num_val float,f180_num_val float,f181_num_val float,f182_num_val float,f183_num_val float,f184_num_val float,f185_num_val float,f186_num_val float,f187_num_val float,f188_num_val float,f189_num_val float,f190_num_val float,f191_num_val float,f192_num_val float,f193_num_val float,f194_num_val float,f195_num_val float,f196_num_val float,f197_num_val float,f198_num_val float,f199_num_val float,f200_num_val float,f201_num_val float,f202_num_val float,f203_num_val float,f204_num_val float,f205_num_val float,f206_num_val float,f207_num_val float,f208_num_val float,f209_num_val float,f210_num_val float,f211_num_val float,f212_num_val float,f213_num_val float,f214_num_val float,f215_num_val float,f216_num_val float,f217_num_val float,f218_num_val float,f219_num_val float,f220_num_val float,f221_num_val float,f222_num_val float,f223_num_val float,f224_num_val float,f225_num_val float,f226_num_val float,f227_num_val float,f228_num_val float,f229_num_val float,f230_num_val float,f231_num_val float,f232_num_val float,f233_num_val float,f234_num_val float,f235_num_val float,f236_num_val float,f237_num_val float,f238_num_val float,f239_num_val float,f240_num_val float,f241_num_val float,f242_num_val float,f243_num_val float,f244_num_val float,f245_num_val float,f246_num_val float,f247_num_val float,f248_num_val float,f249_num_val float,f250_num_val float,f251_num_val float,f252_num_val float,f253_num_val float,f254_num_val float,f255_num_val float,f256_num_val float,f257_num_val float,f258_num_val float,f259_num_val float,f260_num_val float,f261_num_val float,f262_num_val float,f263_num_val float,f264_num_val float,f265_num_val float,f266_num_val float,f267_num_val float,f268_num_val float,f269_num_val float,f270_num_val float,f271_num_val float,f272_num_val float,f273_num_val float,f274_num_val float,f275_num_val float,f276_num_val float,f277_num_val float,f278_num_val float,f279_num_val float,f280_num_val float,f281_num_val float,f282_num_val float,f283_num_val float,f284_num_val float,f285_num_val float,f286_num_val float,f287_num_val float,f288_num_val float,f289_num_val float,f290_num_val float,f291_num_val float,f292_num_val float,f293_num_val float,f294_num_val float,f295_num_val float,f296_num_val float,f297_num_val float,f298_num_val float,f299_num_val float,f300_num_val float,f301_num_val float,f302_num_val float,f303_num_val float,f304_num_val float,f305_num_val float,f306_num_val float,f307_num_val float,f308_num_val float,f309_num_val float,f310_num_val float,f311_num_val float,f312_num_val float,f313_num_val float,f314_num_val float,f315_num_val float,f316_num_val float,f317_num_val float,f318_num_val float,f319_num_val float,f320_num_val float,f321_num_val float,f322_num_val float,f323_num_val float,f324_num_val float,f325_num_val float,f326_num_val float,f327_num_val float,f328_num_val float,f329_num_val float,f330_num_val float,f331_num_val float,f332_num_val float,f333_num_val float,f334_num_val float,f335_num_val float,f336_num_val float,f337_num_val float,f338_num_val float,f339_num_val float,f340_num_val float,f341_num_val float,f342_num_val float,f343_num_val float,f344_num_val float,f345_num_val float,f346_num_val float,f347_num_val float,f348_num_val float,f349_num_val float,f350_num_val float,f351_num_val float,f352_num_val float,f353_num_val float,f354_num_val float,f355_num_val float,f356_num_val float,f357_num_val float,f358_num_val float,f359_num_val float,f360_num_val float,f361_num_val float,f362_num_val float,f363_num_val float,f364_num_val float,f365_num_val float,f366_num_val float,f367_num_val float,f368_num_val float,f369_num_val float,f370_num_val float,f371_num_val float,f372_num_val float,f373_num_val float,f374_num_val float,f375_num_val float,f376_num_val float,f377_num_val float,f378_num_val float,f379_num_val float,f380_num_val float,f381_num_val float,f382_num_val float,f383_num_val float,f384_num_val float,f385_num_val float,f386_num_val float,f387_num_val float,f388_num_val float,f389_num_val float,f390_num_val float,f391_num_val float,f392_num_val float,f393_num_val float,f394_num_val float,f395_num_val float,f396_num_val float,f397_num_val float,f398_num_val float,f399_num_val float,f400_num_val float,f401_num_val float,f402_num_val float,f403_num_val float,f404_num_val float,f405_num_val float,f406_num_val float,f407_num_val float,f408_num_val float,f409_num_val float,f410_num_val float,f411_num_val float,f412_num_val float,f413_num_val float,f414_num_val float,f415_num_val float,f416_num_val float,f417_num_val float,f418_num_val float,f419_num_val float,f420_num_val float,f421_num_val float,f422_num_val float,f423_num_val float,f424_num_val float,f425_num_val float,f426_num_val float,f427_num_val float,f428_num_val float,f429_num_val float,f430_num_val float,f431_num_val float,f432_num_val float,f433_num_val float,f434_num_val float,f435_num_val float,f436_num_val float,f437_num_val float,f438_num_val float,f439_num_val float,f440_num_val float,f441_num_val float,f442_num_val float,f443_num_val float,f444_num_val float,f445_num_val float,f446_num_val float,f447_num_val float,f448_num_val float,f449_num_val float,f450_num_val float,f451_num_val float,f452_num_val float,f453_num_val float,f454_num_val float,f455_num_val float,f456_num_val float,f457_num_val float,f458_num_val float,f459_num_val float,f460_num_val float,f461_num_val float,f462_num_val float,f463_num_val float,f464_num_val float,f465_num_val float,f466_num_val float,f467_num_val float,f468_num_val float,f469_num_val float,f470_num_val float,f471_num_val float,f472_num_val float,f473_num_val float,f474_num_val float,f475_num_val float,f476_num_val float,f477_num_val float,f478_num_val float,f479_num_val float,f480_num_val float,f481_num_val float,f482_num_val float,f483_num_val float,f484_num_val float,f485_num_val float,f486_num_val float,f487_num_val float,f488_num_val float,f489_num_val float,f490_num_val float,f491_num_val float,f492_num_val float,f493_num_val float,f494_num_val float,f495_num_val float,f496_num_val float,f497_num_val float,f498_num_val float,f499_num_val float,f500_num_val float,f501_num_val float,f502_num_val float,f503_num_val float,f504_num_val float,f505_num_val float,f506_num_val float,f507_num_val float,f508_num_val float,f509_num_val float,f510_num_val float,f511_num_val float,f512_num_val float,f513_num_val float,f514_num_val float,f515_num_val float,f516_num_val float,f517_num_val float,f518_num_val float,f519_num_val float,f520_num_val float,f521_num_val float,f522_num_val float,f523_num_val float,f524_num_val float,f525_num_val float,f526_num_val float,f527_num_val float,f528_num_val float,f529_num_val float,f530_num_val float,f531_num_val float,f532_num_val float,f533_num_val float,f534_num_val float,f535_num_val float,f536_num_val float,f537_num_val float,f538_num_val float,f539_num_val float,f540_num_val float,f541_num_val float,f542_num_val float,f543_num_val float,f544_num_val float,f545_num_val float,f546_num_val float,f547_num_val float,f548_num_val float,f549_num_val float,f550_num_val float,f551_num_val float,f552_num_val float,f553_num_val float,f554_num_val float,f555_num_val float,f556_num_val float,f557_num_val float,f558_num_val float,f559_num_val float,f560_num_val float,f561_num_val float,f562_num_val float,f563_num_val float,f564_num_val float,f565_num_val float,f566_num_val float,f567_num_val float,f568_num_val float,f569_num_val float,f570_num_val float,f571_num_val float,f572_num_val float,f573_num_val float,f574_num_val float,f575_num_val float,f576_num_val float,f577_num_val float,f578_num_val float,f579_num_val float,f580_num_val float,f581_num_val float,f582_num_val float,f583_num_val float,f584_num_val float,f585_num_val float,f586_num_val float,f587_num_val float,f588_num_val float,f589_num_val float,f590_num_val float,f591_num_val float,f592_num_val float,f593_num_val float,f594_num_val float,f595_num_val float,f596_num_val float,f597_num_val float,f598_num_val float,f599_num_val float,f600_num_val float,f601_num_val float,f602_num_val float,f603_num_val float,f604_num_val float,f605_num_val float,f606_num_val float,f607_num_val float,f608_num_val float,f609_num_val float,f610_num_val float,f611_num_val float,f612_num_val float,f613_num_val float,f614_num_val float,f615_num_val float,f616_num_val float,f617_num_val float,f618_num_val float,f619_num_val float,f620_num_val float,f621_num_val float,f622_num_val float,f623_num_val float,f624_num_val float,f625_num_val float,f626_num_val float,f627_num_val float,f628_num_val float,f629_num_val float,f630_num_val float,f631_num_val float,f632_num_val float,f633_num_val float,f634_num_val float,f635_num_val float,f636_num_val float,f637_num_val float,f638_num_val float,f639_num_val float,f640_num_val float,f641_num_val float,f642_num_val float,f643_num_val float,f644_num_val float,f645_num_val float,f646_num_val float,f647_num_val float,f648_num_val float,f649_num_val float,f650_num_val float,f651_num_val float,f652_num_val float,f653_num_val float,f654_num_val float,f655_num_val float,f656_num_val float,f657_num_val float,f658_num_val float,f659_num_val float,f660_num_val float,f661_num_val float,f662_num_val float,f663_num_val float,f664_num_val float,f665_num_val float,f666_num_val float,f667_num_val float,f668_num_val float,f669_num_val float,f670_num_val float,f671_num_val float,f672_num_val float,f673_num_val float,f674_num_val float,f675_num_val float,f676_num_val float,f677_num_val float,f678_num_val float,f679_num_val float,f680_num_val float,f681_num_val float,f682_num_val float,f683_num_val float,f684_num_val float,f685_num_val float,f686_num_val float,f687_num_val float,f688_num_val float,f689_num_val float,f690_num_val float,f691_num_val float,f692_num_val float,f693_num_val float,f694_num_val float,f695_num_val float,f696_num_val float,f697_num_val float,f698_num_val float,f699_num_val float,f700_num_val float,f701_num_val float,f702_num_val float,f703_num_val float,f704_num_val float,f705_num_val float,f706_num_val float,f707_num_val float,f708_num_val float,f709_num_val float,f710_num_val float,f711_num_val float,f712_num_val float,f713_num_val float,f714_num_val float,f715_num_val float,f716_num_val float,f717_num_val float,f718_num_val float,f719_num_val float,f720_num_val float,f721_num_val float,f722_num_val float,f723_num_val float,f724_num_val float,f725_num_val float,f726_num_val float,f727_num_val float,f728_num_val float,f729_num_val float,f730_num_val float,f731_num_val float,f732_num_val float,f733_num_val float,f734_num_val float,f735_num_val float,f736_num_val float,f737_num_val float,f738_num_val float,f739_num_val float,f740_num_val float,f741_num_val float,f742_num_val float,f743_num_val float,f744_num_val float,f745_num_val float,f746_num_val float,f747_num_val float,f748_num_val float,f749_num_val float,f750_num_val float,f751_num_val float,f752_num_val float,f753_num_val float,f754_num_val float,f755_num_val float,f756_num_val float,f757_num_val float,f758_num_val float,f759_num_val float,f760_num_val float,f761_num_val float,f762_num_val float,f763_num_val float,f764_num_val float,f765_num_val float,f766_num_val float,f767_num_val float,f768_num_val float,f769_num_val float,f770_num_val float,f771_num_val float,f772_num_val float,f773_num_val float,f774_num_val float,f775_num_val float,f776_num_val float,f777_num_val float,f778_num_val float,f779_num_val float,f780_num_val float,f781_num_val float,f782_num_val float,f783_num_val float,f784_num_val float,f785_num_val float,f786_num_val float,f787_num_val float,f788_num_val float,f789_num_val float,f790_num_val float,f791_num_val float,f792_num_val float,f793_num_val float,f794_num_val float,f795_num_val float,f796_num_val float,f797_num_val float,f798_num_val float,f799_num_val float,f800_num_val float,f801_num_val float,f802_num_val float,f803_num_val float,f804_num_val float,f805_num_val float,f806_num_val float,f807_num_val float,f808_num_val float,f809_num_val float,f810_num_val float,f811_num_val float,f812_num_val float,f813_num_val float,f814_num_val float,f815_num_val float,f816_num_val float,f817_num_val float,f818_num_val float,f819_num_val float,f820_num_val float,f821_num_val float,f822_num_val float,f823_num_val float,f824_num_val float,f825_num_val float,f826_num_val float,f827_num_val float,f828_num_val float,f829_num_val float,f830_num_val float,f831_num_val float,f832_num_val float,f833_num_val float,f834_num_val float,f835_num_val float,f836_num_val float,f837_num_val float,f838_num_val float,f839_num_val float,f840_num_val float,f841_num_val float,f842_num_val float,f843_num_val float,f844_num_val float,f845_num_val float,f846_num_val float,f847_num_val float,f848_num_val float,f849_num_val float,f850_num_val float,f851_num_val float,f852_num_val float,f853_num_val float,f854_num_val float,f855_num_val float,f856_num_val float,f857_num_val float,f858_num_val float,f859_num_val float,f860_num_val float,f861_num_val float,f862_num_val float,f863_num_val float,f864_num_val float,f865_num_val float,f866_num_val float,f867_num_val float,f868_num_val float,f869_num_val float,f870_num_val float,f871_num_val float,f872_num_val float,f873_num_val float,f874_num_val float,f875_num_val float,f876_num_val float,f877_num_val float,f878_num_val float,f879_num_val float,f880_num_val float,f881_num_val float,f882_num_val float,f883_num_val float,f884_num_val float,f885_num_val float,f886_num_val float,f887_num_val float,f888_num_val float,f889_num_val float,f890_num_val float,f891_num_val float,f892_num_val float,f893_num_val float,f894_num_val float,f895_num_val float,f896_num_val float,f897_num_val float,f898_num_val float,f899_num_val float,f900_num_val float,f901_num_val float,f902_num_val float,f903_num_val float,f904_num_val float,f905_num_val float,f906_num_val float,f907_num_val float,f908_num_val float,f909_num_val float,f910_num_val float,f911_num_val float,f912_num_val float,f913_num_val float,f914_num_val float,f915_num_val float,f916_num_val float,f917_num_val float,f918_num_val float,f919_num_val float,f920_num_val float,f921_num_val float,f922_num_val float,f923_num_val float,f924_num_val float,f925_num_val float,f926_num_val float,f927_num_val float,f928_num_val float,f929_num_val float,f930_num_val float,f931_num_val float,f932_num_val float,f933_num_val float,f934_num_val float,f935_num_val float,f936_num_val float,f937_num_val float,f938_num_val float,f939_num_val float,f940_num_val float,f941_num_val float,f942_num_val float,f943_num_val float,f944_num_val float,f945_num_val float,f946_num_val float,f947_num_val float,f948_num_val float,f949_num_val float,f950_num_val float,f951_num_val float,f952_num_val float,f953_num_val float,f954_num_val float,f955_num_val float,f956_num_val float,f957_num_val float,f958_num_val float,f959_num_val float,f960_num_val float,f961_num_val float,f962_num_val float,f963_num_val float,f964_num_val float,f965_num_val float,f966_num_val float,f967_num_val float,f968_num_val float,f969_num_val float,f970_num_val float,f971_num_val float,f972_num_val float,f973_num_val float,f974_num_val float,f975_num_val float,f976_num_val float,f977_num_val float,f978_num_val float,f979_num_val float,f980_num_val float,f981_num_val float,f982_num_val float,f983_num_val float,f984_num_val float,f985_num_val float,f986_num_val float,f987_num_val float,f988_num_val float,f989_num_val float,f990_num_val float,f991_num_val float,f992_num_val float,f993_num_val float,f994_num_val float,f995_num_val float,f996_num_val float,f997_num_val float,f998_num_val float,f999_num_val float,f1000_num_val float,f1001_num_val float,f1002_num_val float,f1003_num_val float,f1004_num_val float,f1005_num_val float,f1006_num_val float,f1007_num_val float,f1008_num_val float,f1009_num_val float,f1010_num_val float,f1011_num_val float,f1012_num_val float,f1013_num_val float,f1014_num_val float,f1015_num_val float,f1016_num_val float,f1017_num_val float,f1018_num_val float,f1019_num_val float,f1020_num_val float,f1021_num_val float,f1022_num_val float,f1023_num_val float,f1024_num_val float,f1025_num_val float,f1026_num_val float,f1027_num_val float,f1028_num_val float,f1029_num_val float,f1030_num_val float,f1031_num_val float,f1032_num_val float,f1033_num_val float,f1034_num_val float,f1035_num_val float,f1036_num_val float,f1037_num_val float,f1038_num_val float,f1039_num_val float,f1040_num_val float,f1041_num_val float,f1042_num_val float,f1043_num_val float,f1044_num_val float,f1045_num_val float,f1046_num_val float,f1047_num_val float,f1048_num_val float,f1049_num_val float,f1050_num_val float,f1051_num_val float,f1052_num_val float,f1053_num_val float,f1054_num_val float,f1055_num_val float,f1056_num_val float,f1057_num_val float,f1058_num_val float,f1059_num_val float,f1060_num_val float,f1061_num_val float,f1062_num_val float,f1063_num_val float,f1064_num_val float,f1065_num_val float,f1066_num_val float,f1067_num_val float,f1068_num_val float,f1069_num_val float,f1070_num_val float,f1071_num_val float,f1072_num_val float,f1073_num_val float,f1074_num_val float,f1075_num_val float,f1076_num_val float,f1077_num_val float,f1078_num_val float,f1079_num_val float,f1080_num_val float,f1081_num_val float,f1082_num_val float,f1083_num_val float,f1084_num_val float,f1085_num_val float,f1086_num_val float,f1087_num_val float,f1088_num_val float,f1089_num_val float,f1090_num_val float,f1091_num_val float,f1092_num_val float,f1093_num_val float,f1094_num_val float,f1095_num_val float,f1096_num_val float,f1097_num_val float,f1098_num_val float,f1099_num_val float,f1100_num_val float,f1101_num_val float,f1102_num_val float,f1103_num_val float,f1104_num_val float,f1105_num_val float,f1106_num_val float,f1107_num_val float,f1108_num_val float,f1109_num_val float,f1110_num_val float,f1111_num_val float,f1112_num_val float,f1113_num_val float,f1114_num_val float,f1115_num_val float,f1116_num_val float,f1117_num_val float,f1118_num_val float,f1119_num_val float,f1120_num_val float,f1121_num_val float,f1122_num_val float,f1123_num_val float,f1124_num_val float,f1125_num_val float,f1126_num_val float,f1127_num_val float,f1128_num_val float,f1129_num_val float,f1130_num_val float,f1131_num_val float,f1132_num_val float,f1133_num_val float,f1134_num_val float,f1135_num_val float,f1136_num_val float,f1137_num_val float,f1138_num_val float,f1139_num_val float,f1140_num_val float,f1141_num_val float,f1142_num_val float,f1143_num_val float,f1144_num_val float,f1145_num_val float,f1146_num_val float,f1147_num_val float,f1148_num_val float,f1149_num_val float,f1150_num_val float,f1151_num_val float,f1152_num_val float,f1153_num_val float,f1154_num_val float,f1155_num_val float,f1156_num_val float,f1157_num_val float,f1158_num_val float,f1159_num_val float,f1160_num_val float,f1161_num_val float,f1162_num_val float,f1163_num_val float,f1164_num_val float,f1165_num_val float,f1166_num_val float,f1167_num_val float,f1168_num_val float,f1169_num_val float,f1170_num_val float,f1171_num_val float,f1172_num_val float,f1173_num_val float,f1174_num_val float,f1175_num_val float,f1176_num_val float,f1177_num_val float,f1178_num_val float,f1179_num_val float,f1180_num_val float,f1181_num_val float,f1182_num_val float,f1183_num_val float,f1184_num_val float,f1185_num_val float,f1186_num_val float,f1187_num_val float,f1188_num_val float,f1189_num_val float,f1190_num_val float,f1191_num_val float,f1192_num_val float,f1193_num_val float,f1194_num_val float,f1195_num_val float,f1196_num_val float,f1197_num_val float,f1198_num_val float,f1199_num_val float,f1200_num_val float,f1201_num_val float,f1202_num_val float,f1203_num_val float,f1204_num_val float,f1205_num_val float,f1206_num_val float,f1207_num_val float,f1208_num_val float,f1209_num_val float,f1210_num_val float,f1211_num_val float,f1212_num_val float,f1213_num_val float,f1214_num_val float,f1215_num_val float,f1216_num_val float,f1217_num_val float,f1218_num_val float,f1219_num_val float,f1220_num_val float,f1221_num_val float,f1222_num_val float,f1223_num_val float,f1224_num_val float,f1225_num_val float,f1226_num_val float,f1227_num_val float,f1228_num_val float,f1229_num_val float,f1230_num_val float,f1231_num_val float,f1232_num_val float,f1233_num_val float,f1234_num_val float,f1235_num_val float,f1236_num_val float,f1237_num_val float,f1238_num_val float,f1239_num_val float,f1240_num_val float,f1241_num_val float,f1242_num_val float,f1243_num_val float,f1244_num_val float,f1245_num_val float,f1246_num_val float,f1247_num_val float,f1248_num_val float,f1249_num_val float,f1250_num_val float,f1251_num_val float,f1252_num_val float,f1253_num_val float,f1254_num_val float,f1255_num_val float,f1256_num_val float,f1257_num_val float,f1258_num_val float,f1259_num_val float,f1260_num_val float,f1261_num_val float,f1262_num_val float,f1263_num_val float,f1264_num_val float,f1265_num_val float,f1266_num_val float,f1267_num_val float,f1268_num_val float,f1269_num_val float,f1270_num_val float,f1271_num_val float,f1272_num_val float,f1273_num_val float,f1274_num_val float,f1275_num_val float,f1276_num_val float,f1277_num_val float,f1278_num_val float,f1279_num_val float,f1280_num_val float,f1281_num_val float,f1282_num_val float,f1283_num_val float,f1284_num_val float,f1285_num_val float,f1286_num_val float,f1287_num_val float,f1288_num_val float,f1289_num_val float,f1290_num_val float,f1291_num_val float,f1292_num_val float,f1293_num_val float,f1294_num_val float,f1295_num_val float,f1296_num_val float,f1297_num_val float,f1298_num_val float,f1299_num_val float,f1300_num_val float,f1301_num_val float,f1302_num_val float,f1303_num_val float,f1304_num_val float,f1305_num_val float,f1306_num_val float,f1307_num_val float,f1308_num_val float,f1309_num_val float,f1310_num_val float,f1311_num_val float,f1312_num_val float,f1313_num_val float,f1314_num_val float,f1315_num_val float,f1316_num_val float,f1317_num_val float,f1318_num_val float,f1319_num_val float,f1320_num_val float,f1321_num_val float,f1322_num_val float,f1323_num_val float,f1324_num_val float,f1325_num_val float,f1326_num_val float,f1327_num_val float,f1328_num_val float,f1329_num_val float,f1330_num_val float,f1331_num_val float,f1332_num_val float,f1333_num_val float,f1334_num_val float,f1335_num_val float,f1336_num_val float,f1337_num_val float,f1338_num_val float,f1339_num_val float,f1340_num_val float,f1341_num_val float,f1342_num_val float,f1343_num_val float,f1344_num_val float,f1345_num_val float,f1346_num_val float,f1347_num_val float,f1348_num_val float,f1349_num_val float,f1350_num_val float,f1351_num_val float,f1352_num_val float,f1353_num_val float,f1354_num_val float,f1355_num_val float,f1356_num_val float,f1357_num_val float,f1358_num_val float,f1359_num_val float,f1360_num_val float,f1361_num_val float,f1362_num_val float,f1363_num_val float,f1364_num_val float,f1365_num_val float,f1366_num_val float,f1367_num_val float,f1368_num_val float,f1369_num_val float,f1370_num_val float,f1371_num_val float,f1372_num_val float,f1373_num_val float,f1374_num_val float,f1375_num_val float,f1376_num_val float,f1377_num_val float,f1378_num_val float,f1379_num_val float,f1380_num_val float,f1381_num_val float,f1382_num_val float,f1383_num_val float,f1384_num_val float,f1385_num_val float,f1386_num_val float,f1387_num_val float,f1388_num_val float,f1389_num_val float,f1390_num_val float,f1391_num_val float,f1392_num_val float,f1393_num_val float,f1394_num_val float,f1395_num_val float,f1396_num_val float,f1397_num_val float,f1398_num_val float,f1399_num_val float,f1400_num_val float,f1401_num_val float,f1402_num_val float,f1403_num_val float,f1404_num_val float,f1405_num_val float,f1406_num_val float,f1407_num_val float,f1408_num_val float,f1409_num_val float,f1410_num_val float,f1411_num_val float,f1412_num_val float,f1413_num_val float,f1414_num_val float,f1415_num_val float,f1416_num_val float,f1417_num_val float,f1418_num_val float,f1419_num_val float,f1420_num_val float,f1421_num_val float,f1422_num_val float,f1423_num_val float,f1424_num_val float,f1425_num_val float,f1426_num_val float,f1427_num_val float,f1428_num_val float,f1429_num_val float,f1430_num_val float,f1431_num_val float,f1432_num_val float,f1433_num_val float,f1434_num_val float,f1435_num_val float,f1436_num_val float,f1437_num_val float,f1438_num_val float,f1439_num_val float,f1440_num_val float,f1441_num_val float,f1442_num_val float,f1443_num_val float,f1444_num_val float,f1445_num_val float,f1446_num_val float,f1447_num_val float,f1448_num_val float,f1449_num_val float,f1450_num_val float,f1451_num_val float,f1452_num_val float,f1453_num_val float,f1454_num_val float,f1455_num_val float,f1456_num_val float,f1457_num_val float,f1458_num_val float,f1459_num_val float,f1460_num_val float,f1461_num_val float,f1462_num_val float,f1463_num_val float,f1464_num_val float,f1465_num_val float,f1466_num_val float,f1467_num_val float,f1468_num_val float,f1469_num_val float,f1470_num_val float,f1471_num_val float,f1472_num_val float,f1473_num_val float,f1474_num_val float,f1475_num_val float,f1476_num_val float,f1477_num_val float,f1478_num_val float,f1479_num_val float,f1480_num_val float,f1481_num_val float,f1482_num_val float,f1483_num_val float,f1484_num_val float,f1485_num_val float,f1486_num_val float,f1487_num_val float,f1488_num_val float,f1489_num_val float,f1490_num_val float,f1491_num_val float,f1492_num_val float,f1493_num_val float,f1494_num_val float,f1495_num_val float,f1496_num_val float,f1497_num_val float,f1498_num_val float,f1499_num_val float
);

alter table phaedra.hca_subwelldata
	add constraint hca_subwelldata_pk
	primary key (well_id, cell_id);

alter table phaedra.hca_subwelldata
	add constraint hca_subwelldata_fk_well
	foreign key (well_id)
	references phaedra.hca_plate_well(well_id)
	on delete cascade;

grant INSERT, UPDATE, DELETE on phaedra.hca_subwelldata to phaedra_role_crud;
grant SELECT on phaedra.hca_subwelldata to phaedra_role_read;

-- -----------------------------------------------------------------------

create table phaedra.hca_subwelldata_feature (
	protocolclass_id bigint not null,
	feature_id bigint not null,
	sequence_nr integer
);

alter table phaedra.hca_subwelldata_feature
	add constraint hca_subwelldata_feature_pk
	primary key (protocolclass_id, feature_id);
	
grant INSERT, UPDATE, DELETE on phaedra.hca_subwelldata_feature to phaedra_role_crud;
grant SELECT on phaedra.hca_subwelldata_feature to phaedra_role_read;

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