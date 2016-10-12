
-- -----------------------------------------------------------------------
-- HCA_UPLOAD_PLATES_TODO
-- -----------------------------------------------------------------------

CREATE OR REPLACE  VIEW HCA_UPLOAD_PLATES_TODO
AS SELECT
	p.plate_id,
    p.barcode,
    e.experiment_id,
    e.experiment_name,
    e.protocol_id,
    pr.protocol_name,
    pr.upload_system
  FROM hca_plate p, hca_experiment e, hca_protocol pr
  WHERE e.experiment_id = p.experiment_id
  AND pr.protocol_id = e.protocol_id
  AND p.approve_status > 1
  AND p.upload_status = 0;
  
-- -----------------------------------------------------------------------
-- HCA_UPLOAD_COMPOUNDS_TODO
-- -----------------------------------------------------------------------

CREATE OR REPLACE  VIEW HCA_UPLOAD_COMPOUNDS_TODO
AS SELECT
	c.platecompound_id,
	p.plate_id,
	p.barcode,
    e.experiment_id,
    e.experiment_name,
    e.protocol_id,
    pr.protocol_name,
    pr.upload_system
  FROM hca_plate_compound c, hca_plate p, hca_experiment e, hca_protocol pr
  WHERE EXISTS (select cc.curve_id from hca_curve_compound cc where cc.platecompound_id = c.platecompound_id)
  AND p.plate_id = c.plate_id
  AND p.approve_status > 1
  AND e.experiment_id = p.experiment_id
  AND pr.protocol_id = e.protocol_id
  AND c.validate_status >= 0
  AND c.upload_status = 0;
    
-- -----------------------------------------------------------------------
-- GRANTS
-- -----------------------------------------------------------------------
  
grant select on HCA_UPLOAD_PLATES_TODO 		to phaedra_role_read;
grant select on HCA_UPLOAD_COMPOUNDS_TODO 	to phaedra_role_read;