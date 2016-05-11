package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.util.SiloStructureUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.Activator;

public abstract class AbstractPasteCommand<T extends PlatformObject> extends AbstractSiloCommand {

	@SuppressWarnings("unchecked")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = Display.getDefault().getActiveShell();

		// First, look for the target group
		ISelection structSelection = HandlerUtil.getCurrentSelection(event);
		SiloStructure struct = SelectionUtils.getFirstObject(structSelection, SiloStructure.class);
		if (struct == null) {
			if (asNewGroup()) {
				Silo silo = getActiveSilo(event);
				try {
					struct = SiloService.getInstance().getSiloAccessor(silo).getSiloStructure();
				} catch (SiloException e) {}
			} else {
				MessageDialog.openInformation(shell, "No group selected", "Cannot paste items: no group selected.");
			}
		}
		if (struct == null) return null;

		// Then, look for the items to add
		ISelection itemSelection = CopyItems.getCurrentSelectionFromClipboard();
		List<T> items = getItems(itemSelection);
		if (items.isEmpty()) {
			MessageDialog.openInformation(shell, "No " + getObjectName() + "s in clipboard",
					"Cannot paste " + getObjectName() + "s: there are currently no " + getObjectName() + "s in the clipboard.");
			return null;
		}
		String lbl = getObjectName() + (items.size() > 1 ? "s" : "");

		// See if this selection came from a (different) Silo
		List<SiloStructure> siloStructures = SelectionUtils.getObjects(itemSelection, SiloStructure.class);
		SiloStructure siloSource = siloStructures.isEmpty() ? null : siloStructures.get(0);

		// Make sure the items are from the same Protocol Class
		Silo silo = struct.getSilo();
		ProtocolClass protocolClass = silo.getProtocolClass();
		for (T item : items) {
			Object adapter = item.getAdapter(ProtocolClass.class);
			if (!protocolClass.equals(adapter)) {
				MessageDialog.openInformation(shell, "Different Protocol Class",
						"Cannot paste " + getObjectName() + "s from a different Protocol Class.");
				return null;
			}
		}

		// Ask user for input/confirmation
		String groupName = null;
		if (asNewGroup()) {
			final SiloStructure root = SiloStructureUtils.getRoot(struct);
			IInputValidator validator = new IInputValidator() {
				@Override
				public String isValid(String newText) {
					for (String group: root.getDataGroups()) {
						if (group.equals("/" + newText)) return "A group with this name already exists";
					}
					return null;
				}
			};
			InputDialog dialog = new InputDialog(shell, "Add Group", "Please enter a name for the new group:", "New Group", validator);
			int retCode = dialog.open();
			if (retCode == Window.CANCEL) return null;
			groupName = "/" + dialog.getValue();
		} else {
			groupName = struct.getFullName();
			boolean confirmed = MessageDialog.openConfirm(shell, "Paste " + lbl, "Do you want to add " + items.size() + " " + lbl + " to " + groupName + "?");
			if (!confirmed) return null;
		}

		// Insert the selection of items as new rows into the target group
		try {
			ISiloAccessor<T> accessor = SiloService.getInstance().getSiloAccessor(silo);
			if (siloSource != null) {
				accessor.addRows(groupName, (T[])items.toArray(new PlatformObject[items.size()]), siloSource);
			} else {
				accessor.addRows(groupName, (T[])items.toArray(new PlatformObject[items.size()]));
			}
		} catch (SiloException e) {
			String msg = "Failed to add " + lbl;
			ErrorDialog.openError(shell, "Cannot add " + lbl, msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}

		return null;
	}

	protected abstract boolean asNewGroup();

	protected abstract String getObjectName();

	protected abstract List<T> getItems(ISelection selection);
}