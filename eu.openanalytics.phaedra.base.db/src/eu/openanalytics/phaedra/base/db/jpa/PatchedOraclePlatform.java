package eu.openanalytics.phaedra.base.db.jpa;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.driver.OracleConnection;
import oracle.sql.BINARY_DOUBLE;
import oracle.sql.CLOB;

import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.platform.database.OraclePlatform;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.JDBCUtils.XMLTypeHolder;

/**
 * Patched version of OraclePlatform (see EclipseLink) that supports
 * the following PreparedStatement object types:
 * <ul>
 * <li>Double into BINARY_DOUBLE</li>
 * <li>XMLTypeHolder into CLOB</li>
 * </ul>
 */
public class PatchedOraclePlatform extends OraclePlatform {

	private static final long serialVersionUID = 8622394217133633409L;
	
	@Override
	public void setParameterValueInDatabaseCall(Object parameter, PreparedStatement statement, int index, AbstractSession session) throws SQLException {
		
		OraclePreparedStatement ops = statement.unwrap(OraclePreparedStatement.class);
		
		if (parameter instanceof Double) {
			ops.setBinaryDouble(index, new BINARY_DOUBLE((Double)parameter));
		} else if (parameter instanceof XMLTypeHolder) {
			XMLTypeHolder holder = (XMLTypeHolder)parameter;
			String xml = holder.xml;
			OracleConnection oc = statement.getConnection().unwrap(OracleConnection.class);
			CLOB clob = JDBCUtils.makeCLOB(xml, oc);
			ops.setCLOB(index, clob);
		} else {
			super.setParameterValueInDatabaseCall(parameter, statement, index, session);
		}
	}
}
