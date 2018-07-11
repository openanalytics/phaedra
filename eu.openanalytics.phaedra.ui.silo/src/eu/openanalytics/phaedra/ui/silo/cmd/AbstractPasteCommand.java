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
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.ui.silo.Activator;

public abstract class AbstractPasteCommand<T extends PlatformObject> extends AbstractSiloCommand {

	@SuppressWarnings("unchecked")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = Display.getDefault().getActiveShell();

		Silo silo = getActiveSilo(event);
		
		// Get a list of items to paste
		List<T> items = getItems(CopyItems.getCurrentSelectionFromClipboard());
		if (items.isEmpty()) {
			MessageDialog.openInformation(shell, "No " + getObjectName() + "s in clipboard",
					"Cannot paste items: there are currently no " + getObjectName() + "s in the clipboard.");
			return null;
		}
		
		// Make sure the items are from the same Protocol Class as the silo
		for (T item : items) {
			Object adapter = item.getAdapter(ProtocolClass.class);
			if (!silo.getProtocolClass().equals(adapter)) {
				MessageDialog.openInformation(shell, "Different Protocol Class",
						"Cannot paste items from a different Protocol Class.");
				return null;
			}
		}
		
		// Get a new or existing dataset to paste into
		SiloDataset dataset = null;
		if (asNewDataset()) {
			IInputValidator validator = new IInputValidator() {
				@Override
				public String isValid(String newText) {
					for (SiloDataset ds: silo.getDatasets()) {
						if (ds.getName().equals(newText)) return "A dataset with this name already exists";
					}
					return null;
				}
			};
			InputDialog dialog = new InputDialog(shell, "New Dataset", "Please enter a name for the new dataset:", "New Dataset", validator);
			if (dialog.open() == Window.CANCEL) return null;
			String datasetName = dialog.getValue();
			try {
				dataset = SiloService.getInstance().getSiloAccessor(silo).createDataset(datasetName);
			} catch (SiloException e) {
				String msg = "Failed to create new dataset";
				ErrorDialog.openError(shell, msg, msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
				return null;
			}
		} else {
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			dataset = SelectionUtils.getFirstObject(selection, SiloDataset.class);
			if (dataset == null) {
				MessageDialog.openInformation(shell, "No Dataset Selected", "Cannot paste items: no dataset selected");
				return null;
			}
			String lbl = getObjectName() + (items.size() > 1 ? "s" : "");
			boolean confirmed = MessageDialog.openConfirm(shell, "Paste " + lbl, "Do you want to add " + items.size() + " " + lbl + " to " + dataset.getName() + "?");
			if (!confirmed) return null;
		}
		
		//TODO See if this selection came from a (different) Silo
//		List<SiloStructure> siloStructures = SelectionUtils.getObjects(itemSelection, SiloStructure.class);
//		SiloStructure siloSource = siloStructures.isEmpty() ? null : siloStructures.get(0);

		// Insert the items as new rows into the target dataset
		try {
			ISiloAccessor<T> accessor = SiloService.getInstance().getSiloAccessor(silo);
			accessor.addRows(dataset.getName(), (T[]) items.toArray(new PlatformObject[items.size()]));
		} catch (SiloException e) {
			String msg = "Failed to paste items";
			ErrorDialog.openError(shell, "Failed to paste items", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}

		return null;
	}

	protected abstract boolean asNewDataset();

	protected abstract String getObjectName();

	protected abstract List<T> getItems(ISelection selection);
}