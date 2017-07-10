package eu.openanalytics.phaedra.ui.silo.cmd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class DownloadHDF5FileCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Silo silo = SelectionUtils.getFirstObject(selection, Silo.class);
		if (silo == null) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell()
					, "No Silo selected", "Please select a Silo first.");
			return null;
		}

		download(silo);

		return null;
	}

	private void download(Silo silo) {
		final String source = SiloDataService.getInstance().getSiloFSPath(silo);

		FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		dialog.setFileName("Silo_" + silo.getName() + ".h5");
		dialog.setFilterExtensions(new String[]{"*.h5"});
		final String destination = dialog.open();

		if (destination == null) return;

		Job downloadJob = new Job("Downloading HDF5 file") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					InputStream input = Screening.getEnvironment().getFileServer().getContents(source);
					long fileSize = Screening.getEnvironment().getFileServer().getLength(source);
					StreamUtils.copyAndClose(input, new FileOutputStream(destination), fileSize, monitor);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				} catch (IOException e) {
					return new Status(IStatus.ERROR, "eu.openanalytics.phaedra.silo.ui", "HDF5 download failed", e);
				}
				return Status.OK_STATUS;
			}
		};
		downloadJob.setUser(true);
		downloadJob.schedule();
	}

}
