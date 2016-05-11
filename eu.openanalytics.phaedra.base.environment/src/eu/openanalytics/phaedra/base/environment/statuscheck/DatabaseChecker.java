package eu.openanalytics.phaedra.base.environment.statuscheck;

import java.sql.Connection;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;

public class DatabaseChecker extends AbstractStatusChecker {

	@Override
	public String getName() {
		return "Database";
	}

	@Override
	public String getDescription() {
		return "The database holds information such as"
				+ " protocol definitions, experiments, plates,"
				+ " well data and curve fit results.";
	}

	@Override
	public char getIconLetter() {
		return 'D';
	}

	@Override
	public TrafficStatus poll() {
		return test();
	}

	@Override
	public TrafficStatus test() {
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			long start = System.currentTimeMillis();
			String response = JDBCUtils.getDBSize(conn);
			long duration = System.currentTimeMillis() - start;
			response += ("\nLatency: " + duration + " ms");
			return new TrafficStatus(TrafficStatus.UP, "OK", response.toString());
		} catch (Exception e) {
			return new TrafficStatus(TrafficStatus.DOWN, "Cannot access database: " + e.getMessage(), null);
		}
	}
}