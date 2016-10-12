-- -----------------------------------------------------------------------
-- hca_upload
-- -----------------------------------------------------------------------

create table hca_upload (
	platecompound_id	number,
	compound_ty			varchar2(10),
	compound_nr			varchar2(25),
	protocol_id			number, 
	protocol_name		varchar2(100),
	experiment_id		number, 
	experiment_name		varchar2(100),
	experiment_dt		date, 
	experiment_user		varchar2(25), 
	plate_id			number,
	plate_barcode		varchar2(64),
	plate_description	varchar2(200), 
	plate_info			varchar2(100),
	data_xml			xmltype,
	upload_system		varchar2(25)
)
tablespace phaedra_d
xmltype data_xml STORE AS CLOB;

grant insert, update, delete on hca_upload to phaedra_role_crud;
grant select on hca_upload to phaedra_role_read;

-- -----------------------------------------------------------------------
-- hca_upload_result
-- -----------------------------------------------------------------------

create table hca_upload_result (
	curve_id			number,
	feature_id			number, 
	feature_name		varchar2(100), 
	result_type			varchar2(25), 
	qualifier			varchar2(25),
	value				varchar2(100)
)
tablespace phaedra_d;

grant insert, update, delete on hca_upload_result to phaedra_role_crud;
grant select on hca_upload_result to phaedra_role_read;

-- -----------------------------------------------------------------------
-- hca_upload_point
-- -----------------------------------------------------------------------
 
create table hca_upload_point (
	well_id				number,
	platecompound_id	number,
	curve_id			number,
	feature_id			number,
	group1				varchar2(150),
	group2				varchar2(150),
	group3				varchar2(150),
	feature_name		varchar2(100), 
	concentration		number, 
	is_valid			number, 
	value				number, 
	normalized			number
)
tablespace phaedra_d;

-- -----------------------------------------------------------------------

CREATE INDEX HCA_UPLOAD_IX_1 ON HCA_UPLOAD(plate_id) TABLESPACE phaedra_i;
CREATE INDEX HCA_UPLOAD_IX_2 ON HCA_UPLOAD(platecompound_id) TABLESPACE phaedra_i;
CREATE INDEX HCA_UPLOAD_POINT_IX_1 ON HCA_UPLOAD_POINT(platecompound_id, feature_id) TABLESPACE phaedra_i;
CREATE INDEX HCA_UPLOAD_RESULT_IX_1 ON HCA_UPLOAD_RESULT(curve_id, feature_id) TABLESPACE phaedra_i;

-- -----------------------------------------------------------------------

grant select, insert, update, delete on hca_upload_point to phaedra_role_crud;
grant select on hca_upload_point to phaedra_role_read;