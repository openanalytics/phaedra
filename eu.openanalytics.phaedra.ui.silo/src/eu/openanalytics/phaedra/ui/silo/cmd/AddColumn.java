package eu.openanalytics.phaedra.ui.silo.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class AddColumn extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		SiloDataset dataset = SelectionUtils.getFirstObject(selection, SiloDataset.class);
		if (dataset == null) return null;
		
		IInputValidator validator = new IInputValidator() {
			@Override
			public String isValid(String newText) {
				for (SiloDatasetColumn column: dataset.getColumns()) {
					if (column.getName().equals(newText)) return "A column with this name already exists";
				}
				return null;
			}
		};
		Shell shell = Display.getDefault().getActiveShell();
		InputDialog dialog = new InputDialog(shell, "Add Column", "Please enter a name for the new column:", "New Column", validator);
		int retCode = dialog.open();
		if (retCode == Window.CANCEL) return null;
		
		String newColumnName = dialog.getValue();
		
		try {
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(dataset.getSilo());
			accessor.createColumn(dataset.getName(), newColumnName, SiloDataType.Float);
		} catch (SiloException e) {
			String msg = "Failed to add new column";
			ErrorDialog.openError(shell, "Cannot Add Column", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		
		return null;
	}

}
