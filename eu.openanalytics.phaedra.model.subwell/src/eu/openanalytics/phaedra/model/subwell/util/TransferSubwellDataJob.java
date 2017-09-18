package eu.openanalytics.phaedra.model.subwell.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
import nl.cwi.monetdb.mcl.io.BufferedMCLReader;
import nl.cwi.monetdb.mcl.io.BufferedMCLWriter;
import nl.cwi.monetdb.mcl.net.MapiSocket;

//Java.type("eu.openanalytics.phaedra.model.subwell.util.TransferSubwellDataJob").execute(1,"jdbc:monetdb://localhost/phaedra","monetdb","monetdb")

public class TransferSubwellDataJob extends Job {

	private int featureCount;
	private long protocolId;
	private String dbURL;
	private String dbUser;
	private String dbPassword;
	
	public TransferSubwellDataJob(long protocolId, String dbURL, String dbUser, String dbPassword) {
		super("Transfer Subwell Data");
		this.featureCount = 1000;
		this.protocolId = protocolId;
		this.dbURL = dbURL;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}
	
	public static void execute(long protocolId, String dbURL, String dbUser, String dbPassword) {
		Job job = new TransferSubwellDataJob(protocolId, dbURL, dbUser, dbPassword);
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
			return status(IStatus.ERROR, "Failed to open database connection", e);
		}

		// Code taken from https://github.com/snaga/monetdb/blob/master/java/example/SQLcopyinto.java
		MapiSocket socket = new MapiSocket();
		try {
			socket.setDatabase("phaedra");
			socket.setLanguage("sql");
			socket.connect("localhost", 50000, "monetdb", "monetdb");
			
			BufferedMCLReader in = socket.getReader();
			BufferedMCLWriter out = socket.getWriter();
			
			for (Plate plate: plates) {
				monitor.subTask("Transferring " + plate);
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				IStatus outcome = processPlate(plate, in, out, monitor);
				if (!outcome.isOK()) return outcome;
			}
		} catch (Exception e) {
			return status(IStatus.ERROR, "Failed to open database connection", e);
		} finally {
			socket.close();
		}
		
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		monitor.done();
		return Status.OK_STATUS;
	}
	
	private IStatus processPlate(Plate plate, BufferedMCLReader in, BufferedMCLWriter out, IProgressMonitor monitor) throws IOException {
		List<SubWellFeature> features = ProtocolUtils.getSubWellFeatures(plate);
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);
		
		String error = in.waitForPrompt();
		if (error != null) return status(IStatus.ERROR, error, null);

		for (Well well: plate.getWells()) {
			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			monitor.subTask("Transferring " + plate + ", " + well);

			out.write('s');
			out.write("COPY INTO phaedra.hca_cell FROM STDIN USING DELIMITERS ',','\\n';");
			out.newLine();
			
			Map<SubWellFeature, Object> dataMap = loadData(well);
			int cellCount = SubWellService.getInstance().getNumberOfCells(well);

			for (int i=0; i<cellCount; i++) {
				StringBuilder value = new StringBuilder();
				value.append(well.getId() + ",");
				value.append(i + ",");

				for (int j = 0; j < featureCount; j++) {
					String append = "null,null,";
					if (j >= features.size()) {
						value.append(append);
						continue;
					}

					SubWellFeature feature = features.get(j);
					Object data = dataMap.get(feature);
					if (data != null) {
						if (feature.isNumeric()) {
							float[] numData = (float[]) data;
							String strVal = String.valueOf(numData[i]);
							if (Float.isNaN(numData[i])) strVal = "null";
							append = strVal + ",null,";
						} else {
							String[] strData = (String[]) data;
							append = "null," + strData[i] + ",";
						}
					}
					value.append(append);
				}
				value.deleteCharAt(value.length() - 1);
				out.write(value.toString());
				out.newLine();
			}
			
			out.writeLine(""); // need this one for synchronisation over flush()
			error = in.waitForPrompt();
			if (error != null) return status(IStatus.ERROR, error, null);
			out.writeLine(""); // server wants more, we're going to tell it, this is it
			error = in.waitForPrompt();
			if (error != null) return status(IStatus.ERROR, error, null);
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
