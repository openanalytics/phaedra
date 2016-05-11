package eu.openanalytics.phaedra.ui.silo.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculationService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.ui.silo.Activator;
import eu.openanalytics.phaedra.ui.silo.dialog.AddJEPColumnDialog;

public class AddJEPColumn extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		SiloStructure struct = SelectionUtils.getFirstObject(selection, SiloStructure.class);
		if (struct == null) return null;
		
		final SiloStructure group = struct.isDataset() ? struct.getParent() : struct;
		
		Shell shell = Display.getDefault().getActiveShell();
		AddJEPColumnDialog dialog = new AddJEPColumnDialog(shell, group);
		int retCode = dialog.open();
		if (retCode == Window.CANCEL) return null;
		
		String newColumnName = dialog.getColumnName();
		String formula = dialog.getFormula();
		
		try {
			Object evaluateArray = JEPCalculationService.getInstance().evaluateArray(formula, group);
			
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(group.getSilo());
			accessor.addColumn(group.getFullName(), newColumnName);
			accessor.replaceColumn(group.getFullName(), newColumnName, evaluateArray);
		} catch (SiloException e) {
			String msg = "Failed to add new column";
			ErrorDialog.openError(shell, "Cannot Add Column", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		} catch (CalculationException e) {
			String msg = e.getMessage();
			ErrorDialog.openError(shell, "Cannot Add Column", msg, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		
		return null;
	}

}
