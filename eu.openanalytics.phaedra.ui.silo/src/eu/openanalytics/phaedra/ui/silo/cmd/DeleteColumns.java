package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class DeleteColumns extends AbstractSiloCommand {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String dataset = getActiveSiloDataset(event);
		if (dataset == null) return null;
		
		String[] columnNames = getActiveColumns(event);
		if (columnNames == null || columnNames.length == 0) return null;
		
		Shell shell = Display.getDefault().getActiveShell();
		boolean confirmed = MessageDialog.openConfirm(shell, "Delete Columns", "Are you sure you want to delete these columns?\n" + Arrays.toString(columnNames));
		if (!confirmed) return null;
		
		try {
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(getActiveSilo(event));
			for (String columnName: columnNames) {
				accessor.removeColumn(dataset, columnName);
			}
		} catch (SiloException e) {
			String msg = "Failed to delete the selected column(s)";
			ErrorDialog.openError(shell, msg, msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		
		return null;
	}

}