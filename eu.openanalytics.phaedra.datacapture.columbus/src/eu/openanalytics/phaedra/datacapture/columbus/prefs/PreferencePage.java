package eu.openanalytics.phaedra.datacapture.columbus.prefs;

import java.util.Arrays;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs.ColumbusLogin;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private TableViewer loginTableViewer;
	private Button addLoginBtn;
	private Button editLoginBtn;
	private Button removeLoginBtn;
	private Button setDefaultBtn;
	
	private ColumbusLogin[] logins;
	private boolean[] modifiedLogins;
	private String defaultLoginId;
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Prefs.getStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		loginTableViewer = new TableViewer(area, SWT.FULL_SELECTION | SWT.BORDER);
		loginTableViewer.getTable().setHeaderVisible(true);
		loginTableViewer.setContentProvider(new ArrayContentProvider());
		loginTableViewer.addDoubleClickListener(e -> editLogin());
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200).applyTo(loginTableViewer.getTable());
		
		TableViewerColumn col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("Default");
		col.getColumn().setWidth(50);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setImage(((ColumbusLogin) cell.getElement()).id.equals(defaultLoginId) ? IconManager.getIconImage("tick.png") : null);
			}
		});
		
		col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("Id");
		col.getColumn().setWidth(100);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(((ColumbusLogin) cell.getElement()).id);
			}
		});
		
		col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("Host");
		col.getColumn().setWidth(130);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(((ColumbusLogin) cell.getElement()).host);
			}
		});
		
		col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("Port");
		col.getColumn().setWidth(50);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(((ColumbusLogin) cell.getElement()).port + "");
			}
		});
		
		col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("Account");
		col.getColumn().setWidth(70);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(((ColumbusLogin) cell.getElement()).username);
			}
		});
		
		col = new TableViewerColumn(loginTableViewer, SWT.LEFT);
		col.getColumn().setText("File share");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				cell.setText(((ColumbusLogin) cell.getElement()).fileShare);
			}
		});
		
		Composite btnContainer = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(btnContainer);
		
		addLoginBtn = new Button(btnContainer, SWT.PUSH);
		addLoginBtn.setText("Add");
		addLoginBtn.setImage(IconManager.getIconImage("add.png"));
		addLoginBtn.addListener(SWT.Selection, e -> addLogin());
		
		editLoginBtn = new Button(btnContainer, SWT.PUSH);
		editLoginBtn.setText("Edit");
		editLoginBtn.setImage(IconManager.getIconImage("pencil.png"));
		editLoginBtn.addListener(SWT.Selection, e -> editLogin());
		
		setDefaultBtn = new Button(btnContainer, SWT.PUSH);
		setDefaultBtn.setText("Set Default");
		setDefaultBtn.setImage(IconManager.getIconImage("tick.png"));
		setDefaultBtn.addListener(SWT.Selection, e -> setDefault());
		
		removeLoginBtn = new Button(btnContainer, SWT.PUSH);
		removeLoginBtn.setText("Remove");
		removeLoginBtn.setImage(IconManager.getIconImage("delete.png"));
		removeLoginBtn.addListener(SWT.Selection, e -> removeLogin());
		initializeValues();
		return area;
	}
	
	@Override
	public boolean performOk() {
		for (int i = 0; i < logins.length; i++) {
			if (modifiedLogins[i]) Prefs.save(logins[i]);
		}
		Prefs.setDefaultLoginId(defaultLoginId);
		return super.performOk();
	}
	
	@Override
	protected void performDefaults() {
		initializeValues();
		super.performDefaults();
	}

	private void initializeValues() {
		String[] ids = Prefs.getLoginIds();
		logins = new ColumbusLogin[ids.length];
		modifiedLogins = new boolean[ids.length];
		for (int i = 0; i < logins.length; i++) logins[i] = Prefs.load(ids[i]);
		defaultLoginId = Prefs.getDefaultLogin().id;
		loginTableViewer.setInput(logins);
	}
	
	private void addLogin() {
		ColumbusLogin login = new ColumbusLogin();
		login.id = "New login";
		login.host = "";
		login.username = "";
		login.password = "";
		login.fileShare = "";
		
		int retCode = new EditColumbusLoginDialog(getShell(), login, true).open();
		if (retCode == Window.OK) {
			logins = Arrays.copyOf(logins, logins.length+1);
			modifiedLogins = Arrays.copyOf(modifiedLogins, modifiedLogins.length+1);
			logins[logins.length-1] = login;
			modifiedLogins[modifiedLogins.length-1] = true;
			loginTableViewer.setInput(logins);
		}
	}
	
	private void editLogin() {
		ColumbusLogin login = SelectionUtils.getFirstObject(loginTableViewer.getSelection(), ColumbusLogin.class);
		if (login == null) {
			MessageDialog.openInformation(getShell(), "No Login Selected", "Select a login to edit.");
			return;
		}
		int index = CollectionUtils.find(logins, login);
		int retCode = new EditColumbusLoginDialog(getShell(), login, false).open();
		if (retCode == Window.OK) {
			modifiedLogins[index] = true;
			loginTableViewer.refresh();
		}
	}

	private void setDefault() {
		ColumbusLogin login = SelectionUtils.getFirstObject(loginTableViewer.getSelection(), ColumbusLogin.class);
		if (login == null) {
			MessageDialog.openInformation(getShell(), "No Login Selected", "Select a login to set as the default login.");
			return;
		}
		defaultLoginId = login.id;
		loginTableViewer.refresh();
	}
	
	private void removeLogin() {
		ColumbusLogin login = SelectionUtils.getFirstObject(loginTableViewer.getSelection(), ColumbusLogin.class);
		if (login == null) {
			MessageDialog.openInformation(getShell(), "No Login Selected", "Select a login to remove.");
			return;
		}
		boolean confirmed = MessageDialog.openConfirm(getShell(), "Remove Login", "Are you sure you want to remove the login " + login.host + "?");
		if (confirmed) {
			ColumbusLogin[] newLogins = new ColumbusLogin[logins.length-1];
			boolean[] newModifiedLogins = new boolean[modifiedLogins.length-1];
			int index = 0;
			for (int i = 0; i < logins.length; i++) {
				if (logins[i] == login) continue;
				newLogins[index] = logins[i];
				newModifiedLogins[index++] = modifiedLogins[i];
			}
			Prefs.remove(login);
			logins = newLogins;
			modifiedLogins = newModifiedLogins;
			if (login.id.equals(defaultLoginId)) defaultLoginId = null;
			loginTableViewer.setInput(logins);
		}
	}
	
	public static class EditColumbusLoginDialog extends TitleAreaDialog {

		private Text idTxt;
		private Text hostTxt;
		private Text portTxt;
		private Text fileShareTxt;
		private Text usernameTxt;
		private Text passwordTxt;
		
		private ColumbusLogin login;
		private boolean isNewLogin;
		
		public EditColumbusLoginDialog(Shell parentShell, ColumbusLogin login, boolean isNewLogin) {
			super(parentShell);
			this.login = login;
			this.isNewLogin = isNewLogin;
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(area);
			
			ModifyListener emptyListener = e -> {
				String value = ((Text) e.widget).getText();
				if (value.isEmpty()) setError("Value cannot be empty");
			};
			
			new Label(area, SWT.NONE).setText("ID:");
			idTxt = new Text(area, SWT.BORDER);
			idTxt.setText(login.id);
			idTxt.addModifyListener(emptyListener);
			idTxt.addModifyListener(e -> {
				boolean duplicate = CollectionUtils.contains(Prefs.getLoginIds(), idTxt.getText());
				setError(duplicate ? "ID already exists" : null);
			});
			idTxt.setEditable(isNewLogin);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(idTxt);
			
			new Label(area, SWT.NONE).setText("Host:");
			hostTxt = new Text(area, SWT.BORDER);
			hostTxt.setText(login.host);
			hostTxt.addModifyListener(emptyListener);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(hostTxt);
			
			new Label(area, SWT.NONE).setText("Port:");
			portTxt = new Text(area, SWT.BORDER);
			portTxt.setText(login.port + "");
			portTxt.addModifyListener(e -> {
				boolean numeric = NumberUtils.isDigit(portTxt.getText());
				setError(numeric ? null : "Port number must be numeric");
			});
			GridDataFactory.fillDefaults().grab(true, false).applyTo(portTxt);
			
			new Label(area, SWT.NONE).setText("Account:");
			usernameTxt = new Text(area, SWT.BORDER);
			usernameTxt.setText(login.username);
			usernameTxt.addModifyListener(emptyListener);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(usernameTxt);
			
			new Label(area, SWT.NONE).setText("Password:");
			passwordTxt = new Text(area, SWT.BORDER);
			passwordTxt.setEchoChar('*');
			if (login.password != null) passwordTxt.setText(login.password);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(passwordTxt);
			
			new Label(area, SWT.NONE).setText("File share:");
			fileShareTxt = new Text(area, SWT.BORDER);
			fileShareTxt.setText(login.fileShare);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fileShareTxt);
			
			setTitle(login.id);
			setMessage("Edit the settings of this login below.");
			return area;
		}
		
		@Override
		protected void okPressed() {
			login.id = idTxt.getText();
			login.host = hostTxt.getText();
			login.port = Integer.valueOf(portTxt.getText());
			login.fileShare = fileShareTxt.getText();
			login.username = usernameTxt.getText();
			login.password = passwordTxt.getText();
			if (login.password.isEmpty()) login.password = null;
			super.okPressed();
		}
		
		private void setError(String msg) {
			setErrorMessage(msg);
			getButton(OK).setEnabled(msg == null);
		}
	}
}
