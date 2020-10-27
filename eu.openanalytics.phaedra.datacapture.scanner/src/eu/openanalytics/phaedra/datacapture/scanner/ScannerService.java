package eu.openanalytics.phaedra.datacapture.scanner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.scanner.internal.ScanJobScheduler;
import eu.openanalytics.phaedra.datacapture.scanner.internal.ScannerTypeRegistry;
import eu.openanalytics.phaedra.datacapture.scanner.model.IScannerType;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;
import eu.openanalytics.phaedra.datacapture.scanner.queue.ScanJobQueue;

public class ScannerService extends BaseJPAService {

	private static ScannerService instance;
	
	private ScannerService() {
		// Hidden constructor.
	}
	
	public static synchronized ScannerService getInstance() {
		if (instance == null) instance = new ScannerService();
		return instance;
	}
	
	public boolean isEnabled() {
		return DataCaptureService.getInstance().isServerEnabled();
	}
	
	public List<ScanJob> getScheduledScanners() {
		List<ScanJob> allScanJobs = getList(ScanJob.class);
		List<ScanJob> scheduledScanJobs = new ArrayList<>();
		for (ScanJob scanJob: allScanJobs) {
			if (scanJob.getSchedule() != null) scheduledScanJobs.add(scanJob);
		}
		return scheduledScanJobs;
	}
	
	public ScanJob getScheduledScanner(long id) {
		String jpql = "SELECT s FROM ScanJob s WHERE s.id = ?1";
		ScanJob scheduledScanJob = getEntity(jpql, ScanJob.class, id);
		return scheduledScanJob;
	}
	
	public void saveAndScheduleScheduledScanner(ScanJob scanJob) throws ScanException {
		long id = scanJob.getId();
		SecurityService.getInstance().checkWithException(Roles.ADMINISTRATOR, scanJob);
		if (scanJob.getSchedule() == null) throw new IllegalArgumentException("Cannot save scheduled scan job: job has no schedule");
		save(scanJob);
		// If creation succeeds, submit/reschedule the job to the scheduler immediately.
		if (id > 0) {
			ScanJobScheduler.rescheduleJob(scanJob);
		} else {
			ScanJobScheduler.scheduleJob(scanJob, null);
		}
	}
	
	public void deleteScheduledScanner(ScanJob scanJob) throws ScanException {
		SecurityService.getInstance().checkWithException(Roles.ADMINISTRATOR, scanJob);
		ScanJobScheduler.removeScheduledJob(scanJob);
		// If removed from the scheduled jobs, remove from the database
		delete(scanJob);
	}
	
	public List<String> getScannerTypeIds() {
		return Lists.newArrayList(ScannerTypeRegistry.getAvailableTypes());
	}
	
	public void executeScanner(ScanJob scanner, IProgressMonitor monitor) throws ScanException {
		if (scanner == null) throw new ScanException("No scanner provided");
		if (scanner.getType() == null) throw new ScanException("No scanner type provided");
		IScannerType type = ScannerTypeRegistry.getScannerType(scanner.getType());
		if (type == null) throw new ScanException("Unknown scanner type: " + scanner.getType());
		
		// Scanner may be a stale copy, refresh to get the latest config.
		refresh(scanner);
		
		if (monitor == null) monitor = new NullProgressMonitor();
		type.run(scanner, monitor);
	}
	
	public boolean queueScanner(ScanJob scanner) {
		return ScanJobQueue.submit(scanner);
	}
}
