package eu.openanalytics.phaedra.model.user.util;

import java.util.Comparator;

public class UserActivitySorter {

	public static Comparator<UserActivity> USER_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			return o1.getUser().getUserName().compareTo(o2.getUser().getUserName());
		}	
	};

	public static Comparator<UserActivity> ACTIVITY_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			// Reverse: recent dates first.
			return o2.getUser().getLastLogon().compareTo(o1.getUser().getLastLogon());
		}	
	};

	public static Comparator<UserActivity> LOGIN_COUNT_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			return o1.getLoginCount() - o2.getLoginCount();
		}
	};

}