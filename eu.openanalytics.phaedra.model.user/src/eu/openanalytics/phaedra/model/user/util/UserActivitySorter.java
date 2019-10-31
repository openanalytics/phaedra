package eu.openanalytics.phaedra.model.user.util;

import java.util.Comparator;

public class UserActivitySorter {

	public static Comparator<UserActivity> USER_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			return o1.getUserName().compareTo(o2.getUserName());
		}	
	};

	public static Comparator<UserActivity> ACTIVITY_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			// Reverse: recent dates first.
			return o2.getLatestSessionDate().compareTo(o1.getLatestSessionDate());
		}	
	};

	public static Comparator<UserActivity> LOGIN_COUNT_SORTER = new Comparator<UserActivity>() {
		@Override
		public int compare(UserActivity o1, UserActivity o2) {
			return o1.getLoginCount() - o2.getLoginCount();
		}
	};

}