package eu.openanalytics.phaedra.ui.subwell.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DownloadHDF5File extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
		if (plate != null) {
			if (plate.isSubWellDataAvailable()) download(plate);
			else MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No subwell data", 
					"No subwell data is available for " + plate + ".");
		}
		return null;
	}
	
	private void download(Plate plate) {
		
		final String source = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
		
		FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		dialog.setFileName("Plate_" + plate.getBarcode() + ".h5");
		dialog.setFilterExtensions(new String[]{"*.h5"});
		final String destination = dialog.open();
		
		if (destination == null) return;
		
		Job downloadJob = new Job("Downloading HDF5 file") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					InputStream input = Screening.getEnvironment().getFileServer().getContents(source);
					OutputStream output = Screening.getEnvironment().getFileServer().getOutputStream(destination);
					StreamUtils.copyAndClose(input, output, Screening.getEnvironment().getFileServer().getLength(source), monitor);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				} catch (IOException e) {
					return new Status(IStatus.ERROR, "eu.openanalytics.phaedra.ui.subwell", "HDF5 download failed", e);
				}
				return Status.OK_STATUS;
			}
		};
		downloadJob.setUser(true);
		downloadJob.schedule();
	}
}