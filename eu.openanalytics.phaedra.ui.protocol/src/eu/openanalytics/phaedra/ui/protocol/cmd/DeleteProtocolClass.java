package eu.openanalytics.phaedra.ui.protocol.cmd;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.Activator;

public class DeleteProtocolClass extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		ProtocolClass protocolClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
		if (protocolClass != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOLCLASS_DELETE, protocolClass);
			if (access) delete(protocolClass);
		}
		return null;
	}
	
	private void delete(final ProtocolClass protocolClass) {
		
		boolean confirm = MessageDialog.openQuestion(
				Display.getDefault().getActiveShell(),
				"Delete Protocol Class",
				"Are you sure you want to delete this protocol class, including all its protocols and experiments?"
				+ "\nWarning! This action cannot be undone.");
		if (!confirm) return;
		
		IRunnableWithProgress deleteAction = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Deleting Protocol Class", IProgressMonitor.UNKNOWN);
				ProtocolService.getInstance().deleteProtocolClass(protocolClass);
				monitor.done();
			}
		};
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			dialog.run(true, false, deleteAction);
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to delete protocol class", e);
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "Delete Failed", null, status);
			EclipseLog.log(status, Activator.getDefault());
		}
	}

}
