package eu.openanalytics.phaedra.ui.link.importer.wizard.page;

import java.io.IOException;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.util.folderbrowser.FolderBrowserFactory;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.link.importer.ImportUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.util.CustomParameterUI;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentContentProvider;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentLabelProvider;


public class SelectExperimentPage extends BaseStatefulWizardPage {

	private TreeViewer experimentViewer;
	private Button createExpBtn;
	private Combo dataCaptureCfgCombo;
	private CustomParameterUI customParameterUI;
	
	private String sourceFolderName;

	public SelectExperimentPage() {
		super("Select Experiment");
		setTitle("Select Experiment");
		setDescription("Select the destination experiment, or create a new one.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);

		experimentViewer = FolderBrowserFactory.createBrowser(container);
		experimentViewer.setContentProvider(new ExperimentContentProvider());
		experimentViewer.setLabelProvider(new ExperimentLabelProvider());
		experimentViewer.setAutoExpandLevel(0);
		experimentViewer.setInput("root");
		
		Tree tree = experimentViewer.getTree();
		GridDataFactory.fillDefaults().grab(true,false).hint(SWT.DEFAULT, 300).applyTo(tree);
		experimentViewer.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Protocol protocol = SelectionUtils.getFirstObject(experimentViewer.getSelection(), Protocol.class);
				createExpBtn.setEnabled(protocol != null);
				if (protocol != null) selectCaptureConfigIdInCombobox(ImportUtils.getCaptureConfigId(protocol));
				checkPageComplete();
			}
		});

		createExpBtn = new Button(container, SWT.PUSH);
		createExpBtn.setText("New...");
		createExpBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewExperiment();
				checkPageComplete();
			}
		});

		Group advancedGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		advancedGroup.setText("Advanced settings");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(advancedGroup);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(advancedGroup);
		
		new Label(advancedGroup, SWT.NONE).setText("Data capture configuration:");

		dataCaptureCfgCombo = new Combo(advancedGroup, SWT.READ_ONLY);
		dataCaptureCfgCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				customParameterUI.load(getSelectedCaptureConfigId());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(dataCaptureCfgCombo);

		customParameterUI = new CustomParameterUI();
		RichTableViewer customParameterViewer = customParameterUI.create(advancedGroup);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).hint(SWT.DEFAULT, 100).applyTo(customParameterViewer.getControl());

		try {
			String[] allConfigs = DataCaptureService.getInstance().getAllCaptureConfigIds();
			dataCaptureCfgCombo.setItems(allConfigs);
			if (dataCaptureCfgCombo.getItemCount() > 0) dataCaptureCfgCombo.select(0);
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Error", "Could not retrieve available data capture configurations from the Phaedra server.\n\nError message:\n"+e.getMessage());
		}
		
		checkPageComplete();
	}

	private void createNewExperiment() {
		Shell shell = experimentViewer.getTree().getShell();
		Protocol protocol = SelectionUtils.getFirstObject(experimentViewer.getSelection(), Protocol.class);
		if (protocol != null) {
			boolean access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_CREATE, protocol);
			if (!access) return;

			InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
					"Create New Experiment","Enter a name for the new experiment:",
					sourceFolderName, null);
			if (dialog.open() != Window.OK) return;

			String expName = dialog.getValue();
			Experiment newExp = PlateService.getInstance().createExperiment(protocol);
			newExp.setName(expName);
			try {
				PlateService.getInstance().updateExperiment(newExp);
			} catch (Throwable t) {
				MessageDialog.openError(shell, "Error!", "Failed to create Experiment.\n\n" + t.getMessage());
				return;
			}

			experimentViewer.setInput("root");
			experimentViewer.reveal(newExp);
			experimentViewer.setSelection(new StructuredSelection(newExp));
		} else {
			MessageDialog.openError(shell, "Invalid selection", "Please select a Protocol to create the new Experiment under.");
		}
	}

	private void selectCaptureConfigIdInCombobox(String captureConfigId) {
		if (captureConfigId == null) return;
		int index = dataCaptureCfgCombo.indexOf(captureConfigId);
		if (index >= 0) dataCaptureCfgCombo.select(index);
	}

	private String getSelectedCaptureConfigId() {
		int index = dataCaptureCfgCombo.getSelectionIndex();
		if (index == -1) return null;
		return dataCaptureCfgCombo.getItem(index);
	}
	
	private void checkPageComplete() {
		Experiment exp = SelectionUtils.getFirstObject(experimentViewer.getSelection(), Experiment.class);
		String dccId = getSelectedCaptureConfigId();
		setPageComplete(exp != null && dccId != null);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		ImportTask task = ((ImportWizardState)state).task;
		if (!task.createNewPlates) createExpBtn.setEnabled(false);
		sourceFolderName = FileUtils.getName(task.sourcePath);

		String dcConfigId = null;
		try { dcConfigId = task.getCaptureConfigId(); } catch (Exception e) {}
		selectCaptureConfigIdInCombobox(dcConfigId);

		if (task.targetExperiment != null) {
			experimentViewer.setSelection(new StructuredSelection(task.targetExperiment), true);
			if (experimentViewer.getSelection().isEmpty()) {
				// If selection is empty, targetExperiment was an unsaved new experiment to pass the Protocol selection.  
				experimentViewer.setSelection(new StructuredSelection(task.targetExperiment.getProtocol()), true);
			}
		}
		
		createExpBtn.setEnabled(SelectionUtils.getFirstObject(experimentViewer.getSelection(), Protocol.class) != null);
		checkPageComplete();
	}

	@Override
	public void collectState(IWizardState state) {
		ImportTask task = ((ImportWizardState)state).task;
		
		Experiment exp = SelectionUtils.getFirstObject(experimentViewer.getSelection(), Experiment.class);
		if (exp != null) task.targetExperiment = exp;

		task.setCaptureConfigId(getSelectedCaptureConfigId());
		
		for (String key: customParameterUI.getCustomParameterKeys()) task.getParameters().put(key, customParameterUI.getParameterValue(key));
	}
}
