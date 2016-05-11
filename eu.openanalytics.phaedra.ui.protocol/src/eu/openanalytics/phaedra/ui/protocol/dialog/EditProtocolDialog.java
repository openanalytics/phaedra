package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.upload.UploadSystemManager;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class EditProtocolDialog extends TitleAreaDialog {

	private Text nameTxt;
	private Text descriptionTxt;
	private ComboViewer teamCmb;
	private ComboViewer uploadSystemCmb;
	
	private Protocol protocol;
	
	public EditProtocolDialog(Shell parentShell, Protocol protocol) {
		super(parentShell);
		this.protocol = protocol;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Protocol");
		newShell.setSize(500, 350);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		
		// Container of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
	
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Name:");
		
		nameTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(nameTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionTxt = new Text(main, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(descriptionTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Team:");
		
		teamCmb = new ComboViewer(new Combo(main, SWT.READ_ONLY));
		teamCmb.setContentProvider(new ArrayContentProvider());
		teamCmb.setLabelProvider(new LabelProvider());
		GridDataFactory.fillDefaults().grab(true,false).applyTo(teamCmb.getCombo());

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Upload System:");

		uploadSystemCmb = new ComboViewer(new Combo(main, SWT.READ_ONLY));
		uploadSystemCmb.setContentProvider(new ArrayContentProvider());
		uploadSystemCmb.setLabelProvider(new LabelProvider());
		GridDataFactory.fillDefaults().grab(true,false).applyTo(uploadSystemCmb.getCombo());
		
		setTitle("Edit Protocol");
		setMessage("Edit the properties of the Protocol below.");

		initializeFields();
		
		return area;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			close();
			return;
		}

		if (buttonId == IDialogConstants.OK_ID) {
			boolean access = SecurityService.getInstance().check(Permissions.PROTOCOL_EDIT, protocol);
			if (access) {
				protocol.setName(nameTxt.getText());
				protocol.setDescription(descriptionTxt.getText());
				String uploadSystem = SelectionUtils.getFirstObject(uploadSystemCmb.getSelection(), String.class);
				if (uploadSystem == null || uploadSystem.isEmpty()) protocol.setUploadSystem(null);
				else protocol.setUploadSystem(uploadSystem.toUpperCase());
				Object o = ((StructuredSelection)teamCmb.getSelection()).getFirstElement();
				String team = (String)o;
				protocol.setTeamCode(team);
				ProtocolService.getInstance().updateProtocol(protocol);
			}
			close();
			return;
		}
		
		super.buttonPressed(buttonId);
	}
	
	private void initializeFields() {
		nameTxt.setText(protocol.getName());
		if (protocol.getDescription() != null) descriptionTxt.setText(protocol.getDescription());
		
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		if (SecurityService.getInstance().isGlobalAdmin()) {
			List<String> teams = new ArrayList<String>(SecurityService.getInstance().getAllTeams());
			Collections.sort(teams);
			teamCmb.setInput(teams);
			teamCmb.getCombo().select(0);
		} else {
			Set<Group> groups = SecurityService.getInstance().getMemberships(currentUser, Permissions.PROTOCOL_CREATE);
			List<String> teams = new ArrayList<String>();
			for (Group g: groups) {
				CollectionUtils.addUnique(teams, g.getTeam());
			}
			teamCmb.setInput(teams);
			if (!teams.isEmpty()) teamCmb.getCombo().select(0);
		}
		
		if (protocol.getTeamCode() != null) {
			teamCmb.setSelection(new StructuredSelection(protocol.getTeamCode()));
		}
		
		String[] systemNames = UploadSystemManager.getInstance().getSystems().stream().map(s -> s.getName()).toArray(i -> new String[i]);
		String[] allSystemNames = new String[systemNames.length + 1];
		allSystemNames[0] = "";
		System.arraycopy(systemNames, 0, allSystemNames, 1, systemNames.length);
		uploadSystemCmb.setInput(allSystemNames);
		if (protocol.getUploadSystem() != null) {
			uploadSystemCmb.setSelection(new StructuredSelection(protocol.getUploadSystem()));
		}
	}
}
