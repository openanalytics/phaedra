package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;

public class GetUsers extends BaseListOperation<User> {

	private long groupId;
	
	public GetUsers(long groupId) {
		this.groupId = groupId;
	}
	
	@Override
	protected String[] getOperationParameters() {
		return new String[] { "groupId", ""+groupId };
	}
	
	@Override
	protected Class<? extends User> getObjectClass() {
		return User.class;
	}
	
	public static class User {
		public String loginname;
		public long userId;
	}
}
