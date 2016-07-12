package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.EditPatternDialog;
import eu.openanalytics.phaedra.ui.cellprofiler.widget.PatternConfig;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class SelectSubWellDataPage extends BaseStatefulWizardPage {

	private Button noDataFileBtn;
	private Button singleDataFileBtn;
	private Button multiDataFileBtn;
	
	private TableViewer dataFileTableViewer;
	private TableViewer dataHeadersTableViewer;
	
	private FolderSelector folderSelector;
	private Label patternLbl;
	private Button editPatternBtn;
	
	private WizardState wizardState;
	
	protected SelectSubWellDataPage() {
		super("Select Subwell Data");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		noDataFileBtn = new Button(area, SWT.RADIO);
		noDataFileBtn.setText("No subwell data");
		noDataFileBtn.addListener(SWT.Selection, e -> toggleMode());
		
		singleDataFileBtn = new Button(area, SWT.RADIO);
		singleDataFileBtn.setText("One data file per plate");
		singleDataFileBtn.addListener(SWT.Selection, e -> toggleMode());
		
		Composite comp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().indent(15, 0).grab(true, false).applyTo(comp);
		GridLayoutFactory.fillDefaults().applyTo(comp);
		
		dataFileTableViewer = new TableViewer(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).applyTo(dataFileTableViewer.getControl());
		dataFileTableViewer.addSelectionChangedListener(e -> singleDataFileSelected());
		dataFileTableViewer.setContentProvider(new ArrayContentProvider());
		
		TableViewerColumn col = new TableViewerColumn(dataFileTableViewer, SWT.NONE);
		col.getColumn().setWidth(350);
		col.getColumn().setText("File");
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() == null) return;
				Path relative = wizardState.selectedFolder.relativize((Path) cell.getElement());
				cell.setText(relative.toString());
			}
		});
		
		new Label(comp, SWT.NONE).setText("Select the column(s) identifying the well:");
		
		dataHeadersTableViewer = new TableViewer(comp, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).applyTo(dataHeadersTableViewer.getControl());
		dataHeadersTableViewer.addSelectionChangedListener(e -> headersSelected());
		dataHeadersTableViewer.setContentProvider(new ArrayContentProvider());
		
		col = new TableViewerColumn(dataHeadersTableViewer, SWT.NONE);
		col.getColumn().setWidth(350);
		col.getColumn().setText("Header");
		col.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() != null) cell.setText(cell.getElement().toString());
			}
		});
		
		multiDataFileBtn = new Button(area, SWT.RADIO);
		multiDataFileBtn.setText("One data file per well");
		multiDataFileBtn.addListener(SWT.Selection, e -> toggleMode());
		
		comp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().indent(15, 0).grab(true, false).applyTo(comp);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);
		
		new Label(comp, SWT.NONE).setText("Folder:");
		folderSelector = new FolderSelector(comp, SWT.NONE);
		folderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				folderSelected();
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(folderSelector);
		
		new Label(comp, SWT.NONE).setText("Pattern:");
		patternLbl = new Label(comp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(patternLbl);
		
		editPatternBtn = new Button(comp, SWT.PUSH);
		editPatternBtn.setText("...");
		editPatternBtn.setToolTipText("Edit pattern");
		editPatternBtn.addListener(SWT.Selection, e -> editPattern());
		
		setTitle("Select Subwell Data");
    	setDescription("(Optional) Select the subwell data file, containing single cell values.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wizardState = (WizardState) state;
		
		if (wizardState.subWellDataFolder == null) wizardState.subWellDataFolder = wizardState.selectedFolder;
		if (wizardState.subWellDataFilePattern == null) wizardState.subWellDataFilePattern = "well(\\d+)\\.csv";
		
		noDataFileBtn.setSelection(!wizardState.includeSubWellData);
		singleDataFileBtn.setSelection(wizardState.includeSubWellData && wizardState.singleSubWellDataFile);
		multiDataFileBtn.setSelection(wizardState.includeSubWellData && !wizardState.singleSubWellDataFile);
		
		dataFileTableViewer.setInput(wizardState.dataFileCandidates);
		
		folderSelector.setSelectedFolder(wizardState.subWellDataFolder.toFile().getAbsolutePath());
		patternLbl.setText(wizardState.subWellDataFilePattern);
		
		validatePage();
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to do.
	}
	
	private void toggleMode() {
		wizardState.includeSubWellData = !noDataFileBtn.getSelection();
		wizardState.singleSubWellDataFile = singleDataFileBtn.getSelection();
		
		dataFileTableViewer.getTable().setEnabled(singleDataFileBtn.getSelection());
		dataHeadersTableViewer.getTable().setEnabled(singleDataFileBtn.getSelection());
		folderSelector.setEnabled(multiDataFileBtn.getSelection());
		editPatternBtn.setEnabled(multiDataFileBtn.getSelection());
		
		validatePage();
	}
	
	private void validatePage() {
		String err = null;
		if (noDataFileBtn.getSelection()) {
			// No further validation required.
		} else if (singleDataFileBtn.getSelection()) {
			if (wizardState.selectedSubWellDataFile == null) {
				err = "Select a data file";
			} else {
				err = analyzeDataFile(wizardState.selectedSubWellDataFile);
				if (err == null && (wizardState.selectedSubWellDataHeaders == null || wizardState.selectedSubWellDataHeaders.length == 0)) {
					err = "Select the column(s) identifying the well";
				}
			}
		} else {
			if (wizardState.subWellDataFolder == null) {
				err = "Select a folder";
			} else if (wizardState.subWellDataFilePattern == null || wizardState.subWellDataFilePattern.isEmpty()) {
				err = "Enter a valid file pattern";
			} else {
				Path sampleDataFile = CellprofilerAnalyzer.INSTANCE.getSample(wizardState.subWellDataFolder, wizardState.subWellDataFilePattern);
				err = analyzeDataFile(sampleDataFile);
			}
		}
		setErrorMessage(err);
		setPageComplete(err == null);
	}
	
	private void singleDataFileSelected() {
		wizardState.selectedSubWellDataFile = SelectionUtils.getFirstObject(dataFileTableViewer.getSelection(), Path.class);
		dataHeadersTableViewer.setSelection(new StructuredSelection());
		validatePage();
	}
	
	private void headersSelected() {
		wizardState.selectedSubWellDataHeaders = SelectionUtils.getObjects(dataHeadersTableViewer.getSelection(), String.class).toArray(new String[0]);
		validatePage();
	}

	private void folderSelected() {
		wizardState.subWellDataFolder = Paths.get(folderSelector.getSelectedFolder());
		validatePage();		
	}
	
	private void editPattern() {
		PatternConfig patternConfig = new PatternConfig();
		patternConfig.pattern = wizardState.subWellDataFilePattern;
		patternConfig.folder = wizardState.subWellDataFolder.toFile().getAbsolutePath();
		int retCode = new EditPatternDialog(getShell(), patternConfig).open();
		if (retCode == Window.OK) {
			wizardState.subWellDataFilePattern = patternConfig.pattern;
			patternLbl.setText(patternConfig.pattern);
			validatePage();
		}
	}
	
	private String analyzeDataFile(Path dataFile) {
		String errMessage = null;
		if (dataFile == null) {
			errMessage = "No matching data file found";
		} else {
			wizardState.selectedSubWellDataFile = dataFile;
			try {
				getContainer().run(true, false, (monitor) -> {
					CellprofilerAnalyzer.INSTANCE.analyzeSubWellDataFile(wizardState, monitor);
					if (wizardState.subWellDataHeaders != null) {
						Display.getDefault().asyncExec(() -> dataHeadersTableViewer.setInput(wizardState.subWellDataHeaders));
					}
				});
			} catch (Exception e) {
				errMessage = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
			}
		}
		return errMessage;
	}
}
