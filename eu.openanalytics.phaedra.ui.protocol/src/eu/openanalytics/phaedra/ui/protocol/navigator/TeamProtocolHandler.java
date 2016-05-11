package eu.openanalytics.phaedra.ui.protocol.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class TeamProtocolHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().equals("team.protocols"));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		
		// Obtain all teams the current user is member of.
		String user = SecurityService.getInstance().getCurrentUserName();
		Set<String> teams = SecurityService.getInstance().getTeams(user);
		
		// Match the set of teams against the protocol owner teams.
		List<Protocol> protocols = ProtocolService.getInstance().getProtocols();
		List<Protocol> teamProtocols = new ArrayList<Protocol>();
		for (Protocol p: protocols) {
			String[] owners = p.getOwners();
			for (String team: teams) {
				boolean match = CollectionUtils.find(owners, team) != -1;
				if (match) {
					teamProtocols.add(p);
					break;
				}
			}
		}

		EditorFactory.getInstance().openEditor(teamProtocols);
	}
}