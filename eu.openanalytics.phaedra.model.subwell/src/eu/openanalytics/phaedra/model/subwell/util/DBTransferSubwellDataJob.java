package eu.openanalytics.phaedra.model.subwell.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

// Java.type("eu.openanalytics.phaedra.model.subwell.util.DBTransferSubwellDataJob").execute(10)

public class DBTransferSubwellDataJob extends Job {

	private int featureCount;
	private long protocolId;
	private String dbURL;
	private String dbUser;
	private String dbPassword;
	
	public DBTransferSubwellDataJob(long protocolId) {
		super("Transfer Subwell Data");
		this.featureCount = 700;
		this.protocolId = protocolId;
//		this.dbURL = "jdbc:postgresql://phaedra.c4rbrvxxyoqu.eu-west-1.rds.amazonaws.com:5432/phaedra";
//		this.dbUser = "phaedra_admin";
//		this.dbPassword = "pahedra$582";
		this.dbURL = "jdbc:postgresql://localhost:5432/phaedra";
		this.dbUser = "phaedra";
		this.dbPassword = "phaedra";
	}
	
	public static void execute(long protocolId) {
		Job job = new DBTransferSubwellDataJob(protocolId);
		job.setUser(true);
		job.schedule();
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Transferring Subwell Data", IProgressMonitor.UNKNOWN);

		monitor.subTask("Looking up protocol");
		Protocol protocol = ProtocolService.getInstance().getProtocol(protocolId);
		if (protocol == null) return status(IStatus.ERROR, "Protocol not found: " + protocolId, null);
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		
		monitor.subTask("Looking up plates");
		List<Plate> plates = PlateService.getInstance().getExperiments(protocol).stream()
			.flatMap(e -> PlateService.getInstance().getPlates(e).stream())
			.collect(Collectors.toList());
		
		monitor.subTask("Connecting to DB");
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			return status(IStatus.ERROR, "Postgres driver not found", e);
		}
		
		try (Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("select * from phaedra.hca_cell limit 1");
			} catch (SQLException e) {
				monitor.subTask("Setting up table");
				StringBuilder sql = new StringBuilder();
				sql.append("create table phaedra.hca_cell (");
				sql.append("wellId bigint, cellId bigint, ");
				for (int i = 0; i < featureCount; i++) {
					sql.append(String.format("f%dNumVal float, f%dStrVal varchar(100),", i, i));
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")");
				try (Statement stmt = conn.createStatement()) {
					stmt.execute(sql.toString());
				}
			}
		} catch (Throwable e) {
			return status(IStatus.ERROR, e.getMessage(), e);
		}

		try (Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPassword)) {
			for (Plate plate: plates) {
				monitor.subTask("Transferring " + plate);
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				IStatus outcome = processPlate(plate, conn, monitor);
				if (!outcome.isOK()) return outcome;
			}
		} catch (Exception e) {
			return status(IStatus.ERROR, e.getMessage(), (e instanceof SQLException ? ((SQLException)e).getNextException() : e.getCause()));
		}
		
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		monitor.done();
		return Status.OK_STATUS;
	}
	
	private IStatus processPlate(Plate plate, Connection conn, IProgressMonitor monitor) throws IOException, SQLException {
		List<SubWellFeature> features = ProtocolUtils.getSubWellFeatures(plate);
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);
		
		for (Well well: plate.getWells()) {
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			monitor.subTask("Transferring " + plate + ", " + well);

			Map<SubWellFeature, Object> dataMap = loadData(well);
			int cellCount = SubWellService.getInstance().getNumberOfCells(well);

			String colNames = "wellId,cellId," + features.stream()
				.map(f -> String.format(f.isNumeric() ? "f%dNumVal" : "f%dStrVal", features.indexOf(f)))
				.collect(Collectors.joining(","));
			String values = "?,?," + features.stream().map(f -> "?").collect(Collectors.joining(","));
			String queryString = "insert into phaedra.hca_cell(" + colNames + ") values (" + values + ")";
			
			try (PreparedStatement pstmt = conn.prepareStatement(queryString)) {
				for (int i=0; i<cellCount; i++) {
					pstmt.setLong(1, well.getId());
					pstmt.setLong(2, i);
					
					for (int j = 0; j < features.size(); j++) {
						SubWellFeature feature = features.get(j);
						Object data = dataMap.get(feature);
						if (data == null) {
							if (feature.isNumeric()) pstmt.setFloat(j+3, Float.NaN);
							else pstmt.setString(j+3, null);
						} else {
							if (feature.isNumeric()) {
								float[] numData = (float[]) data;
								pstmt.setFloat(j+3, numData[i]);
							} else {
								String[] strData = (String[]) data;
								pstmt.setString(j+3, strData[i]);
							}
						}
					}
					
					pstmt.addBatch();
				}
				
				pstmt.executeBatch();
			}
		}
		
		return Status.OK_STATUS;
	}
	
	private Map<SubWellFeature, Object> loadData(Well well) {
		List<SubWellFeature> features = ProtocolUtils.getSubWellFeatures(well);
		Map<SubWellFeature, Object> dataMap = new HashMap<>();
		for (SubWellFeature feature: features) {
			dataMap.put(feature, SubWellService.getInstance().getData(well, feature));
		}
		return dataMap;
	}
	
	private IStatus status(int level, String msg, Throwable cause) {
		return new Status(level, this.getClass().getPackage().getName(), msg, cause);
	}
}
