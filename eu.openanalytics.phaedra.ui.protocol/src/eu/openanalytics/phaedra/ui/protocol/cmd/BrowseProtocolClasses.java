package eu.openanalytics.phaedra.ui.protocol.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class BrowseProtocolClasses extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<ProtocolClass> protocolClasses = ProtocolService.getInstance().getProtocolClasses();
		EditorFactory.getInstance().openEditor(protocolClasses);
		return null;
	}
}
