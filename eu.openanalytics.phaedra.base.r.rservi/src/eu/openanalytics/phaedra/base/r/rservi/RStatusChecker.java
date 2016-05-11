package eu.openanalytics.phaedra.base.r.rservi;

import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;

public class RStatusChecker extends AbstractStatusChecker {

	@Override
	public String getName() {
		return "R Engine";
	}

	@Override
	public String getDescription() {
		return "The R engine is used for statistical computations"
				+ " such as dose-response curve fits and charts.";	
	}

	@Override
	public char getIconLetter() {
		return 'R';
	}

	@Override
	public TrafficStatus poll() {
		boolean running = RService.getInstance().isRunning();
		if (running) return new TrafficStatus(TrafficStatus.UP, "Ok");
		else return new TrafficStatus(TrafficStatus.UNKNOWN, "R Engine unavailable");
	}
}