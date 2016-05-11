package eu.openanalytics.phaedra.ui.columbus.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;
import eu.openanalytics.phaedra.ui.columbus.Activator;
import eu.openanalytics.phaedra.ui.columbus.importwizard.ColumbusImportHelper;

public class ScreenSelector {

	private Composite control;
	private ComboViewer instanceComboViewer;
	private ComboViewer userComboViewer;
	private ComboViewer screenComboViewer;
	
	private ScreenSelectionListener listener;
	private String selectedInstanceId;
	private User selectedUser;
	private Screen selectedScreen;
	
	public ScreenSelector(Composite parent, ScreenSelectionListener listener) {
		this.listener = listener;
	
		control = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(control);
		
		new Label(control, SWT.NONE).setText("Instance:");
		
		instanceComboViewer = new ComboViewer(control, SWT.READ_ONLY);
		instanceComboViewer.setContentProvider(new ArrayContentProvider());
		instanceComboViewer.setLabelProvider(new LabelProvider());
		instanceComboViewer.addSelectionChangedListener(e -> instanceSelected(SelectionUtils.getFirstObject(e.getSelection(), String.class)));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(instanceComboViewer.getControl());
		
		new Label(control, SWT.NONE).setText("User:");
		
		userComboViewer = new ComboViewer(control, SWT.READ_ONLY);
		userComboViewer.setContentProvider(new ArrayContentProvider());
		userComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((User)element).loginname;
			}
		});
		userComboViewer.addSelectionChangedListener(e -> userSelected(SelectionUtils.getFirstObject(e.getSelection(), User.class)));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userComboViewer.getControl());
		
		new Label(control, SWT.NONE).setText("Screen:");
		
		screenComboViewer = new ComboViewer(control, SWT.READ_ONLY);
		screenComboViewer.setContentProvider(new ArrayContentProvider());
		screenComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Screen)element).screenName;
			}
		});
		screenComboViewer.addSelectionChangedListener(e -> screenSelected(SelectionUtils.getFirstObject(e.getSelection(), Screen.class)));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(screenComboViewer.getControl());
	}
	
	public Composite getControl() {
		return control;
	}
	
	public void init() {
		String[] instanceIds = Prefs.getLoginIds();
		instanceComboViewer.setInput(instanceIds);
		if (instanceIds.length > 0) {
			String defaultId = Prefs.getDefaultLogin().id;
			if (defaultId == null) defaultId = instanceIds[0];
			instanceComboViewer.setSelection(new StructuredSelection(defaultId));
		} else {
			String msg = "No Columbus instances defined. Please go to"
					+ "\nWindow > Preferences > Import > Columbus to define a Columbus instance.";
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "No instances defined", null, new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));
		}
	}

	public String getSelectedInstanceId() {
		return selectedInstanceId;
	}
	
	public User getSelectedUser() {
		return selectedUser;
	}
	
	public Screen getSelectedScreen() {
		return selectedScreen;
	}
	
	public static interface ScreenSelectionListener {
		public void screenSelected(Screen screen);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void instanceSelected(String instanceId) {
		selectedInstanceId = instanceId;
		User[] users = ColumbusImportHelper.getColumbusUsers(selectedInstanceId);
		userComboViewer.setInput(users);
		if (users.length > 0) userComboViewer.setSelection(new StructuredSelection(users[0]));
	}
	
	private void userSelected(User user) {
		selectedUser = user;
		screenComboViewer.setInput(ColumbusImportHelper.getColumbusScreens(selectedUser, selectedInstanceId));
	}
	
	private void screenSelected(Screen screen) {
		this.selectedScreen = screen;
		if (listener != null) listener.screenSelected(selectedScreen);
	}

}
