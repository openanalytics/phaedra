package eu.openanalytics.phaedra.ui.protocol.template.simple;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.folderbrowser.FolderBrowserFactory;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class SelectWellDataPage extends BaseStatefulWizardPage {

	private TreeViewer folderBrowser;
	private SimpleProtocolWizardState wizardState;
	
	protected SelectWellDataPage() {
		super("Select Well Data");
	}

	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(area);
		
		folderBrowser = FolderBrowserFactory.createBrowser(area, null, true);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 300).applyTo(folderBrowser.getControl());
		folderBrowser.addSelectionChangedListener(e -> analyzeSelectedFile());
		
		setTitle("Select Well Data");
    	setDescription("Select a file containing aggregated well data.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wizardState = (SimpleProtocolWizardState) state;
		String selectedFolder = String.valueOf(wizardState.parameters.get("selected.folder"));
		folderBrowser.setInput(FolderBrowserFactory.createRoot(new File(selectedFolder)));
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to do.
	}
	
	private void analyzeSelectedFile() {
		File file = SelectionUtils.getFirstObject(folderBrowser.getSelection(), File.class);
		wizardState.parameters.put("welldata.file", file.getAbsolutePath());
		boolean ok = SimpleProtocolAnalyzer.analyzeFile(wizardState, file.getAbsolutePath(), SimpleProtocolAnalyzer.SCRIPT_ANALYZE_WELLDATA, getContainer());
		setPageComplete(ok);
	}
}
