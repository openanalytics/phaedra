package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUserGroups.UserGroup;


public class GetUserGroups extends BaseListOperation<UserGroup> {
	
	@Override
	protected Class<? extends UserGroup> getObjectClass() {
		return UserGroup.class;
	}
	
	public static class UserGroup {
		public String description;
		public String groupName;
		public long groupId;
	}
}
