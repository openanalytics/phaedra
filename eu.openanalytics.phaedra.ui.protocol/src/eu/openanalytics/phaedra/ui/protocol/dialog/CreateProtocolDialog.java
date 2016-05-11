package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class CreateProtocolDialog extends TitleAreaDialog {

	private Text nameTxt;
	private Text descriptionTxt;
	private ComboViewer pClassCmb;
	private ComboViewer teamCmb;
	private ProtocolClass pClass;
	
	public CreateProtocolDialog(Shell parentShell, ProtocolClass pClass) {
		super(parentShell);
		this.pClass = pClass;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create Protocol");
		newShell.setSize(500, 350);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
	
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Protocol Class:");
		
		pClassCmb = new ComboViewer(new Combo(main, SWT.READ_ONLY));
		pClassCmb.setContentProvider(new ArrayContentProvider());
		pClassCmb.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				ProtocolClass pc = (ProtocolClass)element;
				return pc.getName();
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(pClassCmb.getCombo());
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Name:");
		
		nameTxt = new Text(main, SWT.BORDER);
		nameTxt.setText("New Protocol");
		GridDataFactory.fillDefaults().grab(true,false).applyTo(nameTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionTxt = new Text(main, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 100).applyTo(descriptionTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Team:");
		
		teamCmb = new ComboViewer(new Combo(main, SWT.READ_ONLY));
		teamCmb.setContentProvider(new ArrayContentProvider());
		teamCmb.setLabelProvider(new LabelProvider());
		GridDataFactory.fillDefaults().grab(true,false).applyTo(teamCmb.getCombo());
		
		setTitle("Create Protocol");
		setMessage("Create a new Protocol by selecting a Protocol Class and filling out the"
				+ " required properties.");

		initializeCombos();
		
		return main;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", true);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			close();
			return;
		}

		if (buttonId == IDialogConstants.OK_ID) {
			Object o = ((StructuredSelection)pClassCmb.getSelection()).getFirstElement();
			ProtocolClass pClass = (ProtocolClass)o;
			o = ((StructuredSelection)teamCmb.getSelection()).getFirstElement();
			String team = (String)o;
			
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.PROTOCOL_CREATE, pClass);
			if (access) {
				Protocol p = ProtocolService.getInstance().createProtocol(pClass);
				p.setName(nameTxt.getText());
				p.setDescription(descriptionTxt.getText());
				p.setTeamCode(team);
				ProtocolService.getInstance().updateProtocol(p);
			}
			close();
			return;
		}
		
		super.buttonPressed(buttonId);
	}
	
	private void initializeCombos() {
		
		// Initialize protocol class and team combos
		String currentUser = SecurityService.getInstance().getCurrentUserName();
		List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
		List<ProtocolClass> accessiblePClasses = new ArrayList<ProtocolClass>();
		for (ProtocolClass pClass: pClasses) {
			boolean access = SecurityService.getInstance().check(Permissions.PROTOCOL_CREATE, pClass);
			if (access) accessiblePClasses.add(pClass);
		}
		
		if (accessiblePClasses.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), 
				"Access Denied", "You do not have the required permissions to create protocols.");
			close();
		}
		
		Collections.sort(accessiblePClasses, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
		pClassCmb.setInput(accessiblePClasses);
		if (!accessiblePClasses.isEmpty()) {
			if (pClass == null) pClassCmb.getCombo().select(0);
			else pClassCmb.setSelection(new StructuredSelection(pClass));
		}

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
	}
}
