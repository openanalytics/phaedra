package eu.openanalytics.phaedra.ui.protocol.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.text.AnnotatedTextSupport;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.protocol.template.ProtocolTemplateService;
import eu.openanalytics.phaedra.protocol.template.model.TemplateSettingKeys.TemplateSettingKey;
import eu.openanalytics.phaedra.protocol.template.validation.ValidationItem;
import eu.openanalytics.phaedra.protocol.template.validation.ValidationOutcome;
import eu.openanalytics.phaedra.ui.protocol.template.TemplateWizard.WizardState;

public class TemplateWizardPage extends BaseStatefulWizardPage {

	private Button uploadFileBtn;
	private Combo examplesCmb;
	private StyledText textArea;
	private Label statusIconLbl;
	private Label statusLbl;

	private CTabFolder sideTabFolder;
	private CTabItem availableSettingsTab;
	private CTabItem patternTesterTab;
	private CTabItem validationTab;

	private RichTableViewer availableSettingsViewer;
	private RichTableViewer validationViewer;
	private PatternTester patternTester;
	
	private AnnotatedTextSupport annotatedTextSupport;
	private ValidationOutcome currentValidation;
	
	protected TemplateWizardPage() {
		super("Template Wizard");
	}

	@Override
	public void createControl(Composite parent) {
		SplitComposite area = SplitCompositeFactory.getInstance().prepare(null, SplitComposite.MODE_H_1_2).create(parent);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(5,5).numColumns(2).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		
		Composite mainPanel = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).spacing(5,5).numColumns(2).applyTo(mainPanel);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainPanel);
		
		Label label = new Label(mainPanel, SWT.NONE);
		label.setText("Select a preset:");
		
		examplesCmb = new Combo(mainPanel, SWT.READ_ONLY);
		examplesCmb.addListener(SWT.Selection, e -> selectExample());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(examplesCmb);
		
		String[] ids = ProtocolTemplateService.getInstance().getAvailableTemplateIds();
		Arrays.sort(ids);
		examplesCmb.setItems(ids);
		
		label = new Label(mainPanel, SWT.NONE);
		label.setText("Or select a file:");
		
		uploadFileBtn = new Button(mainPanel, SWT.PUSH);
		uploadFileBtn.setText("Browse...");
		uploadFileBtn.addListener(SWT.Selection, e -> uploadFile());
		
		label = new Label(mainPanel, SWT.NONE);
		label.setText("Settings:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
		
		textArea = new StyledText(mainPanel, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		textArea.addModifyListener(e -> setPageComplete(!textArea.getText().isEmpty()));
		GridDataFactory.fillDefaults().grab(true, true).hint(400,300).applyTo(textArea);
		
		annotatedTextSupport = new AnnotatedTextSupport(textArea, () -> recalculateValidation());

		new Label(mainPanel, SWT.NONE);
		Composite statusBar = new Composite(mainPanel, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(statusBar);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(statusBar);
		statusIconLbl = new Label(statusBar, SWT.NONE);
		statusIconLbl.setImage(IconManager.getIconImage("information.png"));
		statusLbl = new Label(statusBar, SWT.NONE);
		statusLbl.setText("Select a preset");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(statusLbl);
		
		// Side panel: tabbed folder
		
		sideTabFolder = new CTabFolder(area, SWT.BORDER | SWT.TOP | SWT.V_SCROLL | SWT.H_SCROLL);
		sideTabFolder.addListener(SWT.Selection, e -> tabChanged(e.item));
		GridDataFactory.fillDefaults().grab(false,true).applyTo(sideTabFolder);
		
		availableSettingsTab = new CTabItem(sideTabFolder, SWT.NONE);
		availableSettingsTab.setText("Available Settings");
		Composite container = new Composite(sideTabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		availableSettingsTab.setControl(container);
		
		availableSettingsViewer = new RichTableViewer(container, SWT.BORDER);
		availableSettingsViewer.setContentProvider(new ArrayContentProvider());
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		configs.add(ColumnConfigFactory.create("Name", e -> ((TemplateSettingKey)e).name, DataType.String, 190));
		configs.add(ColumnConfigFactory.create("Description", e -> ((TemplateSettingKey)e).description, DataType.String, 250));
		availableSettingsViewer.applyColumnConfig(configs);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(availableSettingsViewer.getControl());
		
		validationTab = new CTabItem(sideTabFolder, SWT.NONE);
		validationTab.setText("Validation");
		container = new Composite(sideTabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		validationTab.setControl(container);
		
		validationViewer = new RichTableViewer(container, SWT.BORDER);
		validationViewer.setContentProvider(new ArrayContentProvider());
		configs = new ArrayList<ColumnConfiguration>();
		configs.add(ColumnConfigFactory.create("", e -> TemplateUtils.getImage((ValidationItem)e), 30));
		configs.add(ColumnConfigFactory.create("Description", e -> ((ValidationItem)e).text, DataType.String, 350));
		validationViewer.applyColumnConfig(configs);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(validationViewer.getControl());
		
		patternTesterTab = new CTabItem(sideTabFolder, SWT.NONE);
		patternTesterTab.setText("Pattern Tester");
		container = new Composite(sideTabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		patternTesterTab.setControl(container);
		
		patternTester = new PatternTester();
		Composite testerComp = patternTester.createComposite(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(testerComp);
		
		sideTabFolder.setSelection(availableSettingsTab);
		
		setControl(area);
		setPageComplete(false);
		setTitle("Template Wizard");
		setMessage("You can create a new Protocol by running a Protocol Template."
				+ " \nThe associated Protocol Class and Data Capture Configuration are automatically created as well.");
	}
	
	@Override
	public void collectState(IWizardState state) {
		((WizardState) state).settings = textArea.getText();
	}
	
	private void tabChanged(Widget tab) {
		// Do nothing.
	}
	
	private void recalculateValidation() {
		annotatedTextSupport.clearAnnotations();
		currentValidation = ProtocolTemplateService.getInstance().validateSettings(textArea.getText());
		for (ValidationItem item: currentValidation.getValidationItems()) {
			annotatedTextSupport.addAnnotation(TemplateUtils.createAnnotation(item));
		}
		
		if (currentValidation.getKeys() != null) {
			TemplateSettingKey[] keys = currentValidation.getKeys().getAll();
			Arrays.sort(keys, (k1,k2) -> k1.name.compareTo(k2.name));
			availableSettingsViewer.setInput(keys);
		} else {
			availableSettingsViewer.setInput(null);
		}
		
		validationViewer.setInput(currentValidation.getValidationItems());
		patternTester.loadSettings(currentValidation);
		
		long errors = currentValidation.getValidationItems().stream().filter(i -> i.severity == ValidationItem.SEV_ERROR).count();
		long warnings = currentValidation.getValidationItems().stream().filter(i -> i.severity == ValidationItem.SEV_WARNING).count();
		
		if (errors == 0 && warnings == 0) {
			statusIconLbl.setImage(IconManager.getIconImage("information.png"));
			statusLbl.setText("No problems detected.");
		} else if (errors == 0) {
			statusIconLbl.setImage(IconManager.getIconImage("error.png"));
			statusLbl.setText(warnings + " warning(s) detected.");
		} else {
			statusIconLbl.setImage(IconManager.getIconImage("exclamation.png"));
			statusLbl.setText(errors + " error(s) detected.");
		}
		
		setPageComplete(errors == 0);
	}

	private void selectExample() {
		String templateId = examplesCmb.getText();
		String example = ProtocolTemplateService.getInstance().getExampleSettings(templateId);
		if (example == null) example = "# This template has no example available";
		textArea.setText(example);
	}
	
	private void uploadFile() {
		FileDialog dialog = new FileDialog(this.getShell());
		String filePath = dialog.open();
		if (filePath == null) return;
		try {
			String settings = new String(StreamUtils.readAll(filePath));
			textArea.setText(settings);
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Failed to read file", "Failed to read the selected file: " + e.getMessage());
		}
	}
}
