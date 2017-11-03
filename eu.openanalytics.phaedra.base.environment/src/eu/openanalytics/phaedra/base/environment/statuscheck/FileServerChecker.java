package eu.openanalytics.phaedra.base.environment.statuscheck;

import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class FileServerChecker extends AbstractStatusChecker {


	private String detailedInfo;

	@Override
	public String getName() {
		return "File Server";
	}

	@Override
	public String getDescription() {
		return "The File Server holds information such as"
				+ " image data, sub-well data, scripts, workflows,"
				+ " configuration files and personal preferences.";	
	}

	@Override
	public char getIconLetter() {
		return 'F';
	}

	@Override
	public TrafficStatus poll() {
		double free = 0;
		double total = 0;
		
		SecureFileServer fs = Screening.getEnvironment().getFileServer();
		try {
			fs.dir("/");
			free = (double)fs.getFreeSpace() / (1024*1024*1024);
			total = (double)fs.getTotalSpace() / (1024*1024*1024);
		} catch (IOException e) {
			return new TrafficStatus(TrafficStatus.DOWN, "Cannot access: " + e.getMessage());
		}

		String gbFree = NumberUtils.round(free, 0);
		String gbTotal = NumberUtils.round(total, 0);
		String pctFree = NumberUtils.round(100*(free/total), 2);
		if (free/total > 0.1) {
			return new TrafficStatus(TrafficStatus.UP, "OK\n" + "Free space: " + pctFree + "% (" + gbFree + " / " + gbTotal + " gb)", detailedInfo);
		} else {
			return new TrafficStatus(TrafficStatus.WARN, "Low Disk Space!\n" + "Free space: " + pctFree + "% (" + gbFree + " / " + gbTotal + " gb)", detailedInfo);
		}
	}
}
