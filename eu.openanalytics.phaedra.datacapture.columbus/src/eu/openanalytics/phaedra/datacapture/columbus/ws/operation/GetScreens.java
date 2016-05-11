package eu.openanalytics.phaedra.datacapture.columbus.ws.operation;

import java.util.Date;

import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;

public class GetScreens extends BaseListOperation<Screen> {

	private long userId;
	
	public GetScreens(long userId) {
		this.userId = userId;
	}

	@Override
	protected String[] getOperationParameters() {
		return new String[] { "userId", ""+userId };
	}

	@Override
	protected Class<? extends Screen> getObjectClass() {
		return Screen.class;
	}
	
	public static class Screen {
		public String screenName;
		public String screenDescription;
		public Date screenStartDate;
		public long screenId;
	}
}
