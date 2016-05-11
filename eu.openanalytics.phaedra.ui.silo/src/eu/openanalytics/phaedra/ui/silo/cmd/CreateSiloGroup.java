package eu.openanalytics.phaedra.ui.silo.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.silo.dialog.SiloGroupDialog;

public class CreateSiloGroup extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return execute(event, null, null);
	}
	
	public static void execute() {
		try {
			new CreateSiloGroup().execute(null);
		} catch (ExecutionException e) {}
	}
	
	public static void executeProtocolClass(ProtocolClass pClass, GroupType type) {
		try {
			new CreateSiloGroup().execute(null, pClass, type);
		} catch (ExecutionException e) {}
	}

	private Object execute(ExecutionEvent event, ProtocolClass pClass, GroupType type)  throws ExecutionException {
		if (event != null) {
			// Get the ProtocolClass
			ISelection selection = (ISelection) HandlerUtil.getCurrentSelection(event);
			PlatformObject object = SelectionUtils.getFirstObject(selection, PlatformObject.class);
			if (object != null) {
				pClass = (ProtocolClass) object.getAdapter(ProtocolClass.class);
			}
		}

		// Open the creation dialog for a Silo
		SiloGroupDialog dialog;
		if (type == null) {
			dialog = new SiloGroupDialog(Display.getDefault().getActiveShell(), pClass);
		} else {
			dialog = new SiloGroupDialog(Display.getDefault().getActiveShell(), pClass, type);
		}
		dialog.open();
		
		return null;
	}

}