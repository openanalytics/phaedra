package eu.openanalytics.phaedra.ui.link.importer.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;


public class DeleteReadings extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<PlateReading> readings = SelectionUtils.getObjects(selection, PlateReading.class);
		delete(readings);
		return null;
	}
	
	public static void execute(List<PlateReading> readings) {
		new DeleteReadings().delete(readings);
	}
	
	private void delete(final List<PlateReading> readings) {
		if (readings == null || readings.isEmpty()) return;
		
		// Security check.
		boolean access = true;
		for (PlateReading r: readings) {
			access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_DELETE, r);
			if (!access) break;
		}
		if (!access) return;
		
		// Ask confirmation.
		boolean confirm = MessageDialog.openQuestion(
				Display.getDefault().getActiveShell(),
				"Delete Reading(s)",
				"Are you sure you want to delete these " + readings.size() + " reading(s)?");
		if (!confirm) return;
		
		// Perform deletion in a cancellable job.
		Job deleteJob = new Job("Deleting reading(s)...") {
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Deleting " + readings.size() + " reading(s)...", readings.size());
				for (PlateReading reading: readings) {
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
					monitor.subTask("Deleting reading " + reading.toString());
					DataCaptureService.getInstance().deleteReading(reading);
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			};
		};
		deleteJob.setUser(true);
		deleteJob.schedule();
	}
}
