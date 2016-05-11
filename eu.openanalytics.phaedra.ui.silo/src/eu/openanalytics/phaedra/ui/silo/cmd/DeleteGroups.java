package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
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

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class DeleteGroups extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		List<SiloStructure> structs = SelectionUtils.getObjects(selection, SiloStructure.class);
		if (structs.isEmpty()) return null;
		
		List<SiloStructure> groupsToDelete = new ArrayList<>();
		for (SiloStructure struct: structs) {
			if (struct.isDataset()) CollectionUtils.addUnique(groupsToDelete, struct.getParent());
			else CollectionUtils.addUnique(groupsToDelete, struct);
		}
		if (groupsToDelete.isEmpty()) return null;
		
		String suffix = groupsToDelete.size() > 1 ? "s" : "";
		
		Shell shell = Display.getDefault().getActiveShell();
		boolean confirmed = MessageDialog.openConfirm(shell, "Delete Group" + suffix, "Are you sure you want to delete " + groupsToDelete.size() + " group" + suffix + "?");
		if (!confirmed) return null;
		
		try {
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(groupsToDelete.get(0).getSilo());
			for (SiloStructure group: groupsToDelete) {
				accessor.removeRows(group.getFullName(), null);
			}
		} catch (SiloException e) {
			String msg = "Failed to delete group" + suffix;
			ErrorDialog.openError(shell, "Cannot Delete Group" + suffix, msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		
		return null;
	}

}