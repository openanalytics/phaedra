package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class BrowseProtocols extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection) HandlerUtil.getCurrentSelection(event);
		ProtocolClass protocolClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
		execute(protocolClass);
		return null;
	}

	public static void execute(ProtocolClass protocolClass) {
		EditorFactory.getInstance().openEditor(protocolClass);
	}
}
