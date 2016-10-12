CREATE OR REPLACE PACKAGE hca_utils_pack AS

	PROCEDURE get_tablespace_info (tsName IN VARCHAR2, bytesUsed OUT NUMBER, bytesFree OUT NUMBER);
								
END hca_utils_pack;
/
show errors;


CREATE OR REPLACE PACKAGE BODY hca_utils_pack AS

	PROCEDURE get_tablespace_info (tsName IN VARCHAR2, bytesUsed OUT NUMBER, bytesFree OUT NUMBER) IS
		BEGIN
			SELECT sum(used_space), sum(free_space) INTO bytesUsed, bytesFree FROM (
				SELECT tablespace_name, sum(bytes) AS used_space, 0 AS free_space FROM sys.user_ts_quotas WHERE tablespace_name = tsName GROUP BY tablespace_name
				UNION
				SELECT tablespace_name, 0 AS used_space, sum(bytes) AS free_space FROM sys.user_free_space WHERE tablespace_name = tsName GROUP BY tablespace_name
			);
		EXCEPTION WHEN OTHERS THEN NULL;
	END;

END;
/
show errors;

GRANT EXECUTE ON hca_utils_pack TO phaedra_role_crud;