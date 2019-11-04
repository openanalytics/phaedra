package eu.openanalytics.phaedra.ui.protocol.navigator;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class AllProtocolsHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals("all.protocols"));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		// Note: as an optimization, load protocol classes first so they are cached for the protocol query.
		ProtocolService.getInstance().getProtocolClasses();
		List<Protocol> protocols = ProtocolService.getInstance().getProtocols();
		EditorFactory.getInstance().openEditor(protocols);
	}

}