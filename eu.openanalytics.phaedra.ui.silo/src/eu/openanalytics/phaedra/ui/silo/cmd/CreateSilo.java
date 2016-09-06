package eu.openanalytics.phaedra.ui.silo.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.dialog.SiloDialog;

public class CreateSilo extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return execute(event, null, null);
	}
	
	public static Object execute(ExecutionEvent event, ProtocolClass pClass, GroupType type) {
		if (pClass == null && event != null) {
			ISelection selection = (ISelection) HandlerUtil.getCurrentSelection(event);
			PlatformObject object = SelectionUtils.getFirstObject(selection, PlatformObject.class);
			if (object != null) pClass = (ProtocolClass) object.getAdapter(ProtocolClass.class);
		}

		// Open the creation dialog for a Silo
		SiloDialog dialog = new SiloDialog(Display.getDefault().getActiveShell(), pClass, type);
		int retCode = dialog.open();
		Silo silo = dialog.getSilo();
		if (retCode == Window.CANCEL || silo == null) return null;

		// Open the newly created Silo
		EditorFactory.getInstance().openEditor(silo);
		return null;
	}

}