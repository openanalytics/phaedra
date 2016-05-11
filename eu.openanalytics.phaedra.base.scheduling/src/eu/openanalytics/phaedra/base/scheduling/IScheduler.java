package eu.openanalytics.phaedra.base.scheduling;

import java.util.List;
import java.util.Map;

import org.quartz.JobDetail;
import org.quartz.Trigger;

public interface IScheduler {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".scheduler";
	public final static String ATTR_ID = "id";
	public final static String ATTR_CLASS = "class";
	
	public Map<JobDetail, List<Trigger>> getJobsToTrigger();
}
