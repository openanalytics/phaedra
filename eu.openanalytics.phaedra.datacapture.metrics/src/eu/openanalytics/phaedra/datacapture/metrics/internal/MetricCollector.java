package eu.openanalytics.phaedra.datacapture.metrics.internal;

import java.util.Date;

import org.hyperic.sigar.Sigar;

import eu.openanalytics.phaedra.datacapture.metrics.internal.dao.ServerMetricDAO;
import eu.openanalytics.phaedra.datacapture.metrics.model.ServerMetric;

public class MetricCollector {
	
	// Time interval to save data to database [default = 1 minute]
	private static long timeInterval = 60000 * 5;
	private static Sigar sigar;
	private static boolean running = false;
	
	private static ServerMetricDAO serverMetricDAO;
	private static Thread collectorThread;
	
	public static void setTimeInterval(long milliseconds) {
		if(milliseconds <= 1000) {
			milliseconds = 1000;
		}
		timeInterval = milliseconds;
	}
	
	public static long getTimeInterval() {
		return timeInterval;
	}
	
	private static void setSigar() {
		if(sigar == null) { 
			sigar = new Sigar();
        }
		ServerMetrics.setSigar(sigar);
		NetworkMetrics.setSigar(sigar);
	}
	
	private static void collectorThread() {
		while(running) {
			ServerMetric serverMetric = collectServerMetrics();
			serverMetricDAO.insertValue(serverMetric);
			
			try {
				Thread.sleep(timeInterval);
			} catch (InterruptedException e) {
				running = false;
				break;
			}
		}
	}
	
	public static ServerMetric collectServerMetrics() {
		ServerMetric serverMetric = new ServerMetric();
		
		serverMetric.setCpu(ServerMetrics.getCpu());
		serverMetric.setDiskUsage(ServerMetrics.getDiskUsed());
		serverMetric.setRamUsage(ServerMetrics.getPhysicalUsed());
		
		serverMetric.setUploadSpeed(NetworkMetrics.getUploadSpeed());
		serverMetric.setDownloadSpeed(NetworkMetrics.getDownloadSpeed());
		serverMetric.setTimestamp(new Date(System.currentTimeMillis()));
		
		return serverMetric;
	}
	
	public static boolean startCollectorThread() {
		if(!running){
			running = true;
			setSigar();
			Runnable myRunnable = () -> collectorThread();
	        collectorThread = new Thread(myRunnable, "serverMetricsCollector");
	        collectorThread.start();
		}
		return running;
    }
	
	public static void stopCollectorThread() {
		collectorThread.interrupt();
		try {
			collectorThread.join();
		} catch (InterruptedException e) {
		}
    }
	
	public static boolean checkCollector() {
		return running;
	}
	

}
