package eu.openanalytics.phaedra.ui.columbus.protocolwizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.ui.columbus.protocolwizard.ColumbusProtocolWizard.WizardState;
import eu.openanalytics.phaedra.ui.columbus.util.ScreenSelector;

public class SelectScreenPage extends BaseStatefulWizardPage {

	private ScreenSelector screenSelector;
	private Text protocolNameText;
	private ComboViewer teamComboViewer;
	private Button autoImportBtn;
	
	protected SelectScreenPage() {
		super("Select Screen");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		Group group = new Group(container, SWT.NONE);
		group.setText("Select a Screen");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(group);
		
		screenSelector = new ScreenSelector(group, s -> screenSelected(s));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(screenSelector.getControl());
		
		autoImportBtn = new Button(group, SWT.CHECK);
		autoImportBtn.setText("Import this screen immediately after creating the protocol.");
		GridDataFactory.fillDefaults().indent(50, 0).applyTo(autoImportBtn);
		
		group = new Group(container, SWT.NONE);
		group.setText("Protocol");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(group);
		
		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Name:");
		
		protocolNameText = new Text(group, SWT.BORDER);
		protocolNameText.setText("New Protocol");
		protocolNameText.addModifyListener(e -> checkPageComplete());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(protocolNameText);
		
		lbl = new Label(group, SWT.NONE);
		lbl.setText("Team:");
		
		teamComboViewer = new ComboViewer(group);
		teamComboViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(teamComboViewer.getControl());
		
		setTitle("Select Screen");
    	setDescription("Select a Screen in Columbus. The new Protocol will be configured to match the Screen's structure.");
    	setControl(container);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		if (firstTime) {
			screenSelector.init();
			
			String[] teams = SecurityService.getInstance().getAccessibleTeams().toArray(new String[0]);
			Arrays.sort(teams);
			teamComboViewer.setInput(teams);
			if (teams.length > 0) teamComboViewer.setSelection(new StructuredSelection(teams[0]));
		}
	}
	
	@Override
	public void collectState(IWizardState state) {
		WizardState s = (WizardState) state;
		s.protocolName = protocolNameText.getText();
		s.protocolTeam = teamComboViewer.getCombo().getText();
		s.autoImport = autoImportBtn.getSelection();
	}

	private void checkPageComplete() {
		WizardState s = (WizardState) ((ColumbusProtocolWizard) getWizard()).getState();
		setPageComplete(!protocolNameText.getText().isEmpty() && s.screen != null);
	}
	
	private void screenSelected(Screen screen) {
		WizardState s = (WizardState) ((ColumbusProtocolWizard) getWizard()).getState();
		s.instanceId = screenSelector.getSelectedInstanceId();
		s.user = screenSelector.getSelectedUser();
		s.screen = screenSelector.getSelectedScreen();
		protocolNameText.setText(s.screen.screenName);
		
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					monitor.beginTask("Analyzing screen", IProgressMonitor.UNKNOWN);
					try {
						s.analyze(monitor);
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					}
					monitor.done();
				}
			});
		} catch (Exception e) {
			String errMessage = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
			MessageDialog.openError(getShell(),
					"Failed to analyze screen",
					"An error occurred while analyzing screen " + s.screen.screenName + ":\n" + errMessage);
			s.screen = null;
		}
		
		checkPageComplete();
	}
}
