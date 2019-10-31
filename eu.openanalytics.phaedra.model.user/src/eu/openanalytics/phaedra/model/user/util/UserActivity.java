package eu.openanalytics.phaedra.model.user.util;

import java.util.Date;

import eu.openanalytics.phaedra.base.util.misc.DateUtils;

public class UserActivity {

	private String userName;
	private boolean isActiveToday;
	private int loginCount;
	private Date latestSessionDate;
	private String latestSessionHost;
	private String latestSessionVersion;
	
	public UserActivity(String userName, int loginCount, Date latestSessionDate, String latestSessionHost, String latestSessionVersion) {
		this.userName = userName;
		this.isActiveToday = DateUtils.isToday(latestSessionDate);
		this.loginCount = loginCount;
		this.latestSessionDate = latestSessionDate;
		this.latestSessionHost = latestSessionHost;
		this.latestSessionVersion = latestSessionVersion;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public boolean isActiveToday() {
		return isActiveToday;
	}
	
	public int getLoginCount() {
		return loginCount;
	}
	
	public Date getLatestSessionDate() {
		return latestSessionDate;
	}
	
	public String getLatestSessionHost() {
		return latestSessionHost;
	}
	
	public String getLatestSessionVersion() {
		return latestSessionVersion;
	}
}
