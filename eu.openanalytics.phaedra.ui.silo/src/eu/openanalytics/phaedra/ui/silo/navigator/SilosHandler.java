package eu.openanalytics.phaedra.ui.silo.navigator;

import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

public class SilosHandler extends BaseHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals(SiloProvider.GROUP_ID_SILOS) || element.getId().startsWith(SiloProvider.GROUP_PREFIX_PCLASS));
	}

	@Override
	public void createContextMenu(final IElement element, IMenuManager mgr) {
		addCreateCmds(element, mgr);
	}
	
}