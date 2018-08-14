package eu.openanalytics.phaedra.ui.user;

import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.user.UserService;

public class SelectUserDialog extends TitleAreaDialog {
	
	private String message;
	private TableViewer tableViewer;
	
	private String selectedUser;
	
	public SelectUserDialog(Shell parentShell, String message) {
		super(parentShell);
		this.message = message;
	}
	
	public String getSelectedUser() {
		return selectedUser;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select User");
		newShell.setSize(400,500);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		setMessage(message == null ? "Select a user from the list below" : message);
		setTitle("Select User");
		
		tableViewer = new TableViewer(area);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new LabelProvider());
		tableViewer.addSelectionChangedListener(e -> selectedUser = SelectionUtils.getFirstObject(e.getSelection(), String.class));
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableViewer.getControl());
		
		tableViewer.setInput(UserService.streamableList(UserService.getInstance().getUsers()).stream()
				.filter(u -> u.getUserName() != null && !u.getUserName().isEmpty())
				.map(u -> u.getUserName().toLowerCase())
				.sorted()
				.collect(Collectors.toList()));

		return area;
	}
	
}