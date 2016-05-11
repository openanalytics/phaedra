package eu.openanalytics.phaedra.ui.silo.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.silo.cmd.CreateSilo;
import eu.openanalytics.phaedra.ui.silo.cmd.CreateSiloGroup;

public class SilosHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals(SiloProvider.SILOS) || element.getId().startsWith(SiloProvider.PCLASS));
	}

	@Override
	public void createContextMenu(final IElement element, IMenuManager mgr) {
		Action action = new Action("Create New Silo", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				if (element.getData() instanceof ProtocolClass) {
					GroupType type = GroupType.WELL;
					if (element.getParent().getId().startsWith(SiloProvider.SUBWELL_SILOS)) {
						type = GroupType.SUBWELL;
					}
					ProtocolClass pClass = (ProtocolClass) element.getData();
					CreateSilo.executeProtocolClass(pClass, type);
				} else {
					CreateSilo.execute();
				}
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("silo_add.png"));
		mgr.add(action);
		
		action = new Action("Create New Silo Group", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				if (element.getData() instanceof ProtocolClass) {
					GroupType type = GroupType.WELL;
					if (element.getParent().getId().startsWith(SiloProvider.SUBWELL_SILOS)) {
						type = GroupType.SUBWELL;
					}
					ProtocolClass pClass = (ProtocolClass) element.getData();
					CreateSiloGroup.executeProtocolClass(pClass, type);
				} else {
					CreateSiloGroup.execute();
				}
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("folder_add.png"));
		mgr.add(action);
	}
	
}