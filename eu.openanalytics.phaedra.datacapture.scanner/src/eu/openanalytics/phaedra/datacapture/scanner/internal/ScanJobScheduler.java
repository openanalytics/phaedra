package eu.openanalytics.phaedra.datacapture.scanner.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.scheduling.IScheduler;
import eu.openanalytics.phaedra.base.scheduling.SchedulingService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.scanner.Activator;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;
import eu.openanalytics.phaedra.datacapture.scanner.ScannerService;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

/**
 * Schedules all configured scan jobs upon startup.
 * <p>
 * <b>Important:</b> when the scheduled time is reached, there is no guarantee the scan job will
 * run immediately. Instead, the scan job is offered to the ScanJobQueue, which may
 * or may not execute the job immediately.
 * </p>
 */
public class ScanJobScheduler implements IScheduler {

	@Override
	public Map<JobDetail, List<Trigger>> getJobsToTrigger() {
		Map<JobDetail, List<Trigger>> jobs = new HashMap<>();

		if (!ScannerService.getInstance().isEnabled()) {
			return jobs;
		}
		
		String scanJobNamePatternString = System.getProperty("datacapture.scanjob.pattern");
		Pattern scanJobNamePattern = scanJobNamePatternString == null ? null : Pattern.compile(scanJobNamePatternString);
		
		List<ScanJob> scanJobs = ScannerService.getInstance().getScheduledScanners()
				.stream()
				.filter(s -> scanJobNamePattern == null || scanJobNamePattern.matcher(s.getLabel()).matches())
				.collect(Collectors.toList());
		
		EclipseLog.info("Scheduling " + scanJobs.size() + " scan job(s)", Activator.getDefault());
		for (ScanJob scanJob: scanJobs) {
			try {
				scheduleJob(scanJob, jobs);
			} catch (ScanException e) {
				// Cannot happen here: the job is simply added into the map.
			}
		}
		
		return jobs;
	}
	
	public static class ScanJobSubmitter implements Job {
		@Override
		public void execute(JobExecutionContext ctx) throws JobExecutionException {
			ScanJob scanJob = (ScanJob)ctx.getJobDetail().getJobDataMap().get("scanJob");
			boolean accepted = ScannerService.getInstance().queueScanner(scanJob);
			if (!accepted) {
				EclipseLog.warn("Scheduled scan job refused", Activator.getDefault());
			}
		}
	}

	public static void scheduleJob(ScanJob scanJob, Map<JobDetail, List<Trigger>> jobs) throws ScanException {
		//jobKey (Identity) is the combination of the name (unique in a group) and a group
		JobDetail jobDetail = JobBuilder.newJob(ScanJobSubmitter.class)
				.withIdentity(String.valueOf(scanJob.getId()), "ScanJobs")
				.build();
		jobDetail.getJobDataMap().put("scanJob", scanJob);
		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(String.valueOf(scanJob.getId()), "ScanJobTriggers")
				.withSchedule(CronScheduleBuilder.cronSchedule(scanJob.getSchedule()))
				.build();
		
		if (jobs == null) {
			// Schedule now
			try {
				SchedulingService.getInstance().getScheduler().scheduleJob(jobDetail, trigger);
			} catch (SchedulerException e) {
				throw new ScanException("Cannot schedule scan job: " + e.getMessage(), e);
			}
		} else {
			// Add to map, schedule later
			jobs.put(jobDetail, Lists.newArrayList(trigger));
		}
	}
	
	public static void rescheduleJob(ScanJob scanJob) throws ScanException {
		TriggerKey oldTriggerKey = new TriggerKey(String.valueOf(scanJob.getId()), "ScanJobTriggers");

		Trigger newTrigger = TriggerBuilder.newTrigger()
				.withIdentity(String.valueOf(scanJob.getId()), "ScanJobTriggers")
				.withSchedule(CronScheduleBuilder.cronSchedule(scanJob.getSchedule()))
				.build();
		
		//Replace the old trigger with the new one
		try {
			SchedulingService.getInstance().getScheduler().rescheduleJob(oldTriggerKey, newTrigger);
		} catch (SchedulerException e) {
			throw new ScanException("Cannot reschedule scan job: " + e.getMessage(), e);
		}
	}
	
	public static void removeScheduledJob(ScanJob scanJob) throws ScanException {
		JobKey jobKey = new JobKey(String.valueOf(scanJob.getId()), "ScanJobs");
		
		//remove a job and all of its triggers
		try {
			SchedulingService.getInstance().getScheduler().deleteJob(jobKey);
		} catch (SchedulerException e) {
			throw new ScanException("Cannot remove scheduled scan job: " + e.getMessage(), e);
		}
	}
	
}
