package eu.openanalytics.phaedra.ui.silo.navigator;

import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

public class SilosHandler extends BaseHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals(SiloProvider.SILOS) || element.getId().startsWith(SiloProvider.PCLASS));
	}

	@Override
	public void createContextMenu(final IElement element, IMenuManager mgr) {
		addCreateCmds(element, mgr);
	}
	
}