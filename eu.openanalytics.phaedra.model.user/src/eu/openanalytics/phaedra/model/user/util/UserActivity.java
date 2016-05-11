package eu.openanalytics.phaedra.model.user.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.openanalytics.phaedra.base.util.misc.DateUtils;
import eu.openanalytics.phaedra.model.user.vo.User;
import eu.openanalytics.phaedra.model.user.vo.UserSession;

public class UserActivity {

	private User user;
	
	private boolean isActive;
	private int loginCount;
	
	private List<UserSession> allSessions;
	private List<UserSession> activeSessions;
	
	public UserActivity(User user, List<UserSession> sessions) {
		if (sessions == null) sessions = new ArrayList<>();
		
		this.user = user;
		this.isActive = DateUtils.isToday(user.getLastLogon());
		this.loginCount = sessions.size();
		this.allSessions = new ArrayList<>(sessions);
		this.activeSessions = new ArrayList<>();
		
		for (UserSession session: sessions) {
			if (DateUtils.isToday(session.getLoginDate())) {
				activeSessions.add(session);
			}
		}
		
		// Order: most recent session first.
		Collections.sort(activeSessions, new Comparator<UserSession>() {
			@Override
			public int compare(UserSession o1, UserSession o2) {
				return o2.getLoginDate().compareTo(o1.getLoginDate());
			}
		});
	}
	
	public User getUser() {
		return user;
	}
	public boolean isActive() {
		return isActive;
	}
	public int getLoginCount() {
		return loginCount;
	}
	public List<UserSession> getAllSessions() {
		return allSessions;
	}
	public List<UserSession> getActiveSessions() {
		return activeSessions;
	}
}
