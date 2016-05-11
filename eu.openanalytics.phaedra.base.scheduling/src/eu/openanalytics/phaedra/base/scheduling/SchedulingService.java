package eu.openanalytics.phaedra.base.scheduling;

import java.util.Properties;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulingService {
	private static final SchedulingService INSTANCE = new SchedulingService();
	
	private Scheduler scheduler;
	
	private SchedulingService() {
	}
	
	public static final SchedulingService getInstance() {
		return INSTANCE;
	}
	
	public Scheduler getScheduler() {
		return scheduler;
	}

	protected void initialize() throws SchedulerException {
		Properties props = new Properties();
		props.put("org.quartz.threadPool.threadCount", "3");
		props.put("org.quartz.scheduler.skipUpdateCheck", "true");
		this.scheduler = new StdSchedulerFactory(props).getScheduler();
	}

}
