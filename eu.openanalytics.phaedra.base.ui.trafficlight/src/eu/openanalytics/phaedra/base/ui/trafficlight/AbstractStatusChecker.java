package eu.openanalytics.phaedra.base.ui.trafficlight;

public abstract class AbstractStatusChecker implements IStatusChecker {

	@Override
	public long getPollInterval() {
		return 10000;
	}

	@Override
	public TrafficStatus test() {
		TrafficStatus status = poll();
		return new TrafficStatus(
				status.getCode()
				, status.getMessage()
				, "There is no suitable test for this section yet."
		);
	}
}
