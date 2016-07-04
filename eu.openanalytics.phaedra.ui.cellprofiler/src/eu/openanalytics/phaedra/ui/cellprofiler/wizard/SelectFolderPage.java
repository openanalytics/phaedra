package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class SelectFolderPage extends BaseStatefulWizardPage {

	private FolderSelector folderSelector;
	private Text protocolNameText;
	private ComboViewer teamComboViewer;
	private Button autoImportBtn;
	
	private WizardState wstate;
	
	protected SelectFolderPage() {
		super("Select Folder");
	}

	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		folderSelector = new FolderSelector(area, SWT.NONE);
		folderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String folder = (String) e.data;
				analyzeSelectedFolder(folder);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(folderSelector);
		
		autoImportBtn = new Button(area, SWT.CHECK);
		autoImportBtn.setText("Import this plate immediately after creating the protocol.");
		GridDataFactory.fillDefaults().indent(20, 0).applyTo(autoImportBtn);
		
		Group group = new Group(area, SWT.NONE);
		group.setText("Protocol");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(group);
		
		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Name:");
		
		protocolNameText = new Text(group, SWT.BORDER);
		protocolNameText.setText("New Protocol");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(protocolNameText);
		
		lbl = new Label(group, SWT.NONE);
		lbl.setText("Team:");
		
		teamComboViewer = new ComboViewer(group);
		teamComboViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(teamComboViewer.getControl());
		
		setTitle("Select Folder");
    	setDescription("Select a folder containing plate data.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wstate = (WizardState) state;
		if (wstate.selectedFolder != null) folderSelector.setSelectedFolder(wstate.selectedFolder.toFile().getAbsolutePath());
		
		if (firstTime) {
			String[] teams = SecurityService.getInstance().getAccessibleTeams().toArray(new String[0]);
			Arrays.sort(teams);
			teamComboViewer.setInput(teams);
			if (teams.length > 0) teamComboViewer.setSelection(new StructuredSelection(teams[0]));
		}
	}
	
	@Override
	public void collectState(IWizardState state) {
		wstate.protocolName = protocolNameText.getText();
		wstate.protocolTeam = teamComboViewer.getCombo().getText();
		wstate.autoImport = autoImportBtn.getSelection();
	}
	
	private void analyzeSelectedFolder(String folder) {
		if (folder.isEmpty()) {
			setPageComplete(false);
			return;
		}
		wstate.selectedFolder = Paths.get(folder);
		protocolNameText.setText(wstate.selectedFolder.getParent().getFileName().toString());
		
		String errMessage = null;
		try {
			getContainer().run(true, false, (monitor) -> new CellprofilerAnalyzer().analyzeFolder(wstate, monitor));
		} catch (Exception e) {
			errMessage = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
		}
		setErrorMessage(errMessage);
		setPageComplete(errMessage == null);
	}
}
