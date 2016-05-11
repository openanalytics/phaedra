package eu.openanalytics.phaedra.base.environment.statuscheck;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Enumeration;

import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;

public class NetworkChecker extends AbstractStatusChecker {

	@Override
	public String getName() {
		return "Network";
	}

	@Override
	public String getDescription() {
		return "The network is required to access database and file servers,"
				+ " and other remote data locations.";
	}

	@Override
	public char getIconLetter() {
		return 'N';
	}

	@Override
	public TrafficStatus poll() {
		try {
			Enumeration<?> interfaces = NetworkInterface.getNetworkInterfaces();
			boolean hasInterfaces = interfaces != null && interfaces.hasMoreElements();
			if (!hasInterfaces) {
				return new TrafficStatus(TrafficStatus.DOWN, "No network interface found");
			}
		} catch (IOException e) {
			return new TrafficStatus(TrafficStatus.DOWN, "Network interface error: " + e.getMessage());
		}
		return new TrafficStatus(TrafficStatus.UP, "Ok");
	}
	

}
