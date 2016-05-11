package eu.openanalytics.phaedra.base.environment.statuscheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.ui.trafficlight.AbstractStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
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
		SecureFileServer fs = Screening.getEnvironment().getFileServer();
		try {
			fs.dir("/");
		} catch (IOException e) {
			return new TrafficStatus(TrafficStatus.DOWN, "Cannot access: " + e.getMessage());
		}

		File fsRoot = new File(fs.getBasePath());
		double free = (double)fsRoot.getFreeSpace() / (1024*1024*1024);
		double total = (double)fsRoot.getTotalSpace() / (1024*1024*1024);
		String gbFree = NumberUtils.round(free, 0);
		String gbTotal = NumberUtils.round(total, 0);
		String pctFree = NumberUtils.round(100*(free/total), 2);
		if (free/total > 0.1) {
			return new TrafficStatus(TrafficStatus.UP, "OK\n" + "Free space: " + pctFree + "% (" + gbFree + " / " + gbTotal + " gb)", detailedInfo);
		} else {
			return new TrafficStatus(TrafficStatus.WARN, "Low Disk Space!\n" + "Free space: " + pctFree + "% (" + gbFree + " / " + gbTotal + " gb)", detailedInfo);
		}
	}

	@Override
	public TrafficStatus test() {
		TrafficStatus status = poll();
		if (status.getCode() == TrafficStatus.DOWN) return status;

		SecureFileServer fs = Screening.getEnvironment().getFileServer();
		File testFile = new File(fs.getBasePath() + "\\utils\\1MB.txt");
		if (testFile.isFile()) {
			long startTime = System.nanoTime();
			try( FileInputStream in = new FileInputStream(testFile)) {
				StreamUtils.readAll(in);
			} catch (IOException e) {
				return new TrafficStatus(TrafficStatus.DOWN, status.getMessage() + "\nCan't find the test file on the server.");
			}
			long diff = System.nanoTime() - startTime;
			DecimalFormat format = new DecimalFormat(".00");
			detailedInfo = "Download speed: " + format.format((1000f / diff * 1000 * 1000)) + " MB/s";
			return new TrafficStatus(status.getCode(), status.getMessage(), detailedInfo);
		} else {
			return new TrafficStatus(TrafficStatus.WARN, "Cannot perform test", "The test download file is not available.");
		}
	}
}
