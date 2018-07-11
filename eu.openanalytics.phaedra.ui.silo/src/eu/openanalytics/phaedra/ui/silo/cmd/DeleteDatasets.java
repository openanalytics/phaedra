package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class DeleteDatasets extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection) HandlerUtil.getCurrentSelection(event);
		List<SiloDataset> datasets = SelectionUtils.getObjects(selection, SiloDataset.class);
		if (datasets.isEmpty()) return null;

		Shell shell = Display.getDefault().getActiveShell();
		boolean confirmed = MessageDialog.openConfirm(shell, "Delete Datasets", "Are you sure you want to delete " + datasets.size() + " dataset(s)?");
		if (!confirmed) return null;

		try {
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(datasets.get(0).getSilo());
			for (SiloDataset ds: datasets) {
				accessor.removeDataset(ds.getName());
			}
		} catch (SiloException e) {
			String msg = "Failed to delete dataset";
			ErrorDialog.openError(shell, msg, msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}

		return null;
	}

}