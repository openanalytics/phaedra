package eu.openanalytics.phaedra.ui.user;

import java.io.IOException;

import eu.openanalytics.phaedra.base.ui.richtableviewer.state.MementoStateStore;
import eu.openanalytics.phaedra.model.user.UserService;


public class UserPreferenceStateStore extends MementoStateStore {
	
	
	public UserPreferenceStateStore() {
	}
	
	
	@Override
	protected String loadStateString(final String tableKey) throws IOException {
		final UserService userService = UserService.getInstance();
		return userService.getPreferenceValue("Memento", tableKey);
	}
	
	@Override
	protected boolean saveStateString(final  String tableKey, final String stateString) {
		final UserService userService = UserService.getInstance();
		return userService.setPreferenceValue("Memento", tableKey, stateString);
	}
	
}
