package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
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
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class DeleteRows extends AbstractSiloCommand {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = Display.getDefault().getActiveShell();

		// Obtain the rows to delete
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<PlatformObject> objects = SelectionUtils.getObjects(selection, PlatformObject.class);
		if (objects.isEmpty()) return null;

		//TODO Properly ignore selections from the tree viewer.
		List<PlatformObject> rowsToDelete = new ArrayList<>();
		for (PlatformObject o : objects) {
			if (!(o instanceof SiloStructure)) rowsToDelete.add(o);
		}
		if (rowsToDelete.isEmpty()) return null;

		//  Obtain the group to delete the rows from
		Silo silo = getActiveSilo(event);
		String dataGroup = getActiveSiloGroup(event);
		if (silo == null || dataGroup == null) {
			MessageDialog.openInformation(shell, "No group selected", "Cannot delete items: no group selected.");
			return null;
		}

		boolean confirmed = MessageDialog.openConfirm(shell, "Delete Rows", "Are you sure you want to delete these " + rowsToDelete.size() + " row(s) ?");
		if (!confirmed) return null;

		try {
			ISiloAccessor<PlatformObject> accessor = SiloService.getInstance().getSiloAccessor(silo);
			int[] rows = new int[rowsToDelete.size()];
			for (int i=0; i<rows.length; i++) {
				rows[i] = accessor.getRow(dataGroup, rowsToDelete.get(i));
			}
			accessor.removeRows(dataGroup, rows);
		} catch (SiloException e) {
			String msg = "Failed to delete rows";
			ErrorDialog.openError(shell, "Cannot Delete Rows", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}

		return null;
	}

}