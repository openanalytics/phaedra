package eu.openanalytics.phaedra.ui.protocol.editor.page.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.UserContext;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class EditorsContentProvider implements ITreeContentProvider {

	private ProtocolClass protocolClass;
	private Set<UserContext> permittedUsers;

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ProtocolClass) {
			String[] owners = protocolClass.getOwners();
			if (owners == null) return null;
			boolean globalGroupPresent = CollectionUtils.find(owners, Group.GLOBAL_TEAM) != -1;
			int size = globalGroupPresent ? owners.length : owners.length + 1;
			Team[] teams = new Team[size];
			int offset = 0;
			if (!globalGroupPresent) {
				teams[0] = new Team(Group.GLOBAL_TEAM);
				offset = 1;
			}
			for (int i=0; i<owners.length; i++) {
				teams[i+offset] = new Team(owners[i]);
			}
			return teams;
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Team) {
			Team team = (Team)parentElement;
			List<String> roleNames = new ArrayList<String>();
			for (UserContext ctx: permittedUsers) {
				for (Group group: ctx.getGroups()) {
					if (group.getTeam().equals(team.name)) {
						CollectionUtils.addUnique(roleNames, group.getRole());
					}
				}
			}
			Collections.sort(roleNames);
			Role[] roles = new Role[roleNames.size()];
			for (int i=0; i<roleNames.size(); i++) {
				roles[i] = new Role(roleNames.get(i), (Team)parentElement);
			}
			return roles;
		} else if (parentElement instanceof Role) {
			Role role = (Role)parentElement;
			List<String> users = new ArrayList<String>();
			for (UserContext ctx: permittedUsers) {
				for (Group group: ctx.getGroups()) {
					if (group.getTeam().equals(role.team.name)
							&& group.getRole().equals(role.name)) {
						CollectionUtils.addUnique(users, ctx.getUsername());
					}
				}
			}
			Collections.sort(users);
			return users.toArray(new String[users.size()]);
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof ProtocolClass || element instanceof Team
				|| element instanceof Role);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		permittedUsers = new HashSet<UserContext>();
		protocolClass = (ProtocolClass)newInput;

		Set<UserContext> allUsers = SecurityService.getInstance().getAllUsers();
		if (protocolClass != null) {
			for (UserContext ctx: allUsers) {
				if (ProtocolService.getInstance().canEditProtocolClass(ctx.getUsername(), protocolClass)) {
					permittedUsers.add(ctx);
				}
			}
		}
	}

	@Override
	public void dispose() {
		// Nothing to dispose.
	}

	public static class Team {
		public String name;

		public Team(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class Role {
		public String name;
		public Team team;

		public Role(String name, Team team) {
			this.name = name;
			this.team = team;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
