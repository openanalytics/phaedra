package eu.openanalytics.phaedra.link.platedef.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.postgresql.util.PGobject;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.subwell.Activator;

/**
 * TODO
 * ITemplateManager uses the term 'id' for the unique identifier for templates, which is a string.
 * But in this class, 'id' is a numeric (auto-incrementing via a sequence) primary key.
 * This numeric id is kept internal here, and the template name is used as the unique identifier instead,
 * to comply with the ITemplateManager design.
 */
public class DBTemplateManager extends AbstractTemplateManager {

	@Override
	public PlateTemplate getTemplate(String id) throws IOException {
		return select("select * from phaedra.hca_plate_template where template_name = '" + id + "'", rs -> {
			if (rs.next()) return parseTemplate(rs);
			else return null;
		}, 0);
	}

	@Override
	public List<PlateTemplate> getTemplates(long protocolClassId) throws IOException {
		return select("select * from phaedra.hca_plate_template where protocolclass_id = " + protocolClassId, rs -> {
			List<PlateTemplate> templates = new ArrayList<>();
			while (rs.next()) templates.add(parseTemplate(rs));
			return templates;
		}, 0);
	}

	@Override
	public boolean exists(String id) {
		return select("select 1 from phaedra.hca_plate_template where template_name = '" + id + "'", rs -> rs.next(), 0);
	}

	@Override
	protected void doDelete(PlateTemplate template) throws IOException {
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("delete from phaedra.hca_plate_template where template_name = '" + template.getId() + "'");
			}
		} catch (SQLException e) {
			throw new IOException("Failed to delete plate template " + template.getId(), e);
		}
	}
	
	@Override
	protected void doCreate(PlateTemplate template, String xmlData) throws IOException {
		String sql = "insert into phaedra.hca_plate_template"
				+ " (template_id, protocolclass_id, template_name, rows, columns, creator, data_xml)"
				+ " values(" + JDBCUtils.getSequenceNextValSQL("phaedra.hca_plate_template_s") + ",?,?,?,?,?,?)";
		
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, template.getProtocolClassId());
				stmt.setString(2, template.getId());
				stmt.setInt(3, template.getRows());
				stmt.setInt(4, template.getColumns());
				stmt.setString(5, template.getCreator());
				
				if (JDBCUtils.isPostgres()) {
					PGobject xmlObject = new PGobject();
					xmlObject.setType("xml");
					xmlObject.setValue(xmlData);
					stmt.setObject(6, xmlObject);
				} else {
					stmt.setString(6, xmlData);
				}
				
				stmt.execute();
				conn.commit();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IOException("Failed to save plate template " + template.getId(), e);
		}
	}
	
	@Override
	protected void doUpdate(PlateTemplate template, String xmlData) throws IOException {
		String sql = "update phaedra.hca_plate_template"
				+ " set protocolclass_id = ?, rows = ?, columns = ?, creator = ?, data_xml = ?"
				+ " where template_name = ?";
		
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, template.getProtocolClassId());
				stmt.setInt(2, template.getRows());
				stmt.setInt(3, template.getColumns());
				stmt.setString(4, template.getCreator());
				
				if (JDBCUtils.isPostgres()) {
					PGobject xmlObject = new PGobject();
					xmlObject.setType("xml");
					xmlObject.setValue(xmlData);
					stmt.setObject(5, xmlObject);
				} else {
					stmt.setString(5, xmlData);
				}
				
				stmt.setString(6, template.getId());
				stmt.execute();
				conn.commit();
			}
		} catch (SQLException e) {
			throw new IOException("Failed to save plate template " + template.getId(), e);
		}
	}
	
	private PlateTemplate parseTemplate(ResultSet rs) throws SQLException {
		try {
			String xml = rs.getString("data_xml");
			PlateTemplate template = TemplateParser.parse(new ByteArrayInputStream(xml.getBytes()));
			template.setId(rs.getString("template_name"));
			template.setProtocolClassId(rs.getLong("protocolclass_id"));
			template.setRows(rs.getInt("rows"));
			template.setColumns(rs.getInt("columns"));
			template.setCreator(rs.getString("creator"));
			return template;
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse plate template XML", e);
		}
	}
	
	private Connection getConnection() {
		return Screening.getEnvironment().getJDBCConnection();
	}
	
	private <T> T select(String sql, ResultProcessor<T> resultProcessor, int fetchSize) {
		long start = System.currentTimeMillis();
		try (Connection conn = getConnection()) {
			try (Statement stmt = conn.createStatement()) {
				if (fetchSize > 0) stmt.setFetchSize(fetchSize);
				try (ResultSet rs = stmt.executeQuery(sql)) {
					return resultProcessor.process(rs);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to run query: " + sql, e);
		} finally {
			long duration = System.currentTimeMillis() - start;
			EclipseLog.info(String.format("Query took %d ms: %s", duration, sql), Platform.getBundle(Activator.class.getPackage().getName()));
		}
	}
	
	private interface ResultProcessor<T> {
		public T process(ResultSet rs) throws SQLException;
	}
}
