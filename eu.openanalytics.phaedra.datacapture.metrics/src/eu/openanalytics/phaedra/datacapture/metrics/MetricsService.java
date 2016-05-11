package eu.openanalytics.phaedra.datacapture.metrics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.datacapture.metrics.internal.MetricCollector;
import eu.openanalytics.phaedra.datacapture.metrics.internal.NetworkMetrics;
import eu.openanalytics.phaedra.datacapture.metrics.internal.ServerMetrics;
import eu.openanalytics.phaedra.datacapture.metrics.internal.dao.ServerMetricDAO;
import eu.openanalytics.phaedra.datacapture.metrics.model.ServerMetric;

public class MetricsService {

	private static MetricsService instance;
	private ServerMetricDAO serverMetricDAO;
	
	private MetricsService() {
		serverMetricDAO = new ServerMetricDAO(getEntityManager());
	}
	
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	public static synchronized MetricsService getInstance() {
		if (instance == null) instance = new MetricsService();
		return instance;
	}
	
	public List<ServerMetric> getMetrics(Date from, Date to, MetricInterval interval, MetricMethod method) {
		if(interval == null) interval = MetricInterval.Day;
		if(from == null) from = new Date(0);
		if(to == null) to = new Date();
		
		// TODO remove when this starts with server
		startMetricCollector();
		
		Map<MetricInterval, String> timeFormats = new HashMap<MetricInterval,String>();
		timeFormats.put(MetricInterval.Minute, "m/k d/M/yyyy");
		timeFormats.put(MetricInterval.Hour, "k d/M/yyyy");
		timeFormats.put(MetricInterval.Day, "d/M/yyyy");
		timeFormats.put(MetricInterval.Week, "w/yyyy");
		timeFormats.put(MetricInterval.Month, "M/yyyy");
		DateFormat dformat = new SimpleDateFormat(timeFormats.get(interval));
		
		List<ServerMetric> metricsHistory = serverMetricDAO.getValues(from, to);
		
		List<ServerMetric> returnList = new ArrayList<ServerMetric>();
		metricsHistory.stream()
			.collect(Collectors.groupingBy( metrics -> dformat.format(metrics.getTimestamp()) ))
			.forEach((timeValue, metrics) -> {
            	ServerMetric metricObject = new ServerMetric();
            	
            	long time = 0;
                try{time = dformat.parse((String) timeValue).getTime();
                } catch (Exception e) {}
                
            	metricObject.setTimestamp(new Date(time));
            	metricObject.setDiskUsage( formatMethod(metrics, e -> e.getDiskUsage(), method) );
            	metricObject.setCpu( formatMethod(metrics, e -> (long)e.getCpu(), method) );
            	metricObject.setDownloadSpeed( formatMethod(metrics, e -> e.getDownloadSpeed(), method) );
            	metricObject.setUploadSpeed( formatMethod(metrics, e -> e.getUploadSpeed(), method) );
            	metricObject.setRamUsage( formatMethod(metrics, e -> e.getRamUsage(), method) );
            	
            	returnList.add(metricObject);
            });
		
		return returnList;
	}
	
	private static long formatMethod(Collection<ServerMetric> metrics, ToLongFunction<ServerMetric> getMetricValue, MetricMethod method) {
		LongSummaryStatistics metricStats = metrics.stream().mapToLong(getMetricValue).summaryStatistics();

		if(method == null) method = MetricMethod.Average;
		switch(method) {
		case Average:
			return (long)metricStats.getAverage();
		case Max:
			return metricStats.getMax();
		case Min:
			return metricStats.getMin();
		default:
			return 0;
		}
	}
	
	public boolean startMetricCollector() {
		return MetricCollector.startCollectorThread();
	}
	
	public void stopMetricCollector() {
		MetricCollector.stopCollectorThread();
	}
	
	public boolean checkMetricCollector() {
		return MetricCollector.checkCollector();
	}
	
	public void setCollectorInterval(long milliseconds) {
		MetricCollector.setTimeInterval(milliseconds);
		MetricCollector.startCollectorThread();
	}
	
	public long getCollectorInterval() {
		return MetricCollector.getTimeInterval();
	}
	
	public ServerMetric getMetrics() {
		return MetricCollector.collectServerMetrics();
	}
	
	public long getTotalPhysicalMemory() {
		return ServerMetrics.getPhysicalTotal();
	}
	
	public long getTotalDiskSpace() {
		return ServerMetrics.getDiskTotal();
	}
	
	public long getBandwith() {
		return NetworkMetrics.getBandwidth();
	}
}
