package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class SelectFolderPage extends BaseStatefulWizardPage {

	private FolderSelector folderSelector;
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
		
		setTitle("Select Folder");
    	setDescription("Select a folder containing plate data.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		wstate = (WizardState) state;
		if (wstate.selectedFolder != null) folderSelector.setSelectedFolder(wstate.selectedFolder.toFile().getAbsolutePath());
	}
	
	@Override
	public void collectState(IWizardState state) {
		// Nothing to do.
	}
	
	private void analyzeSelectedFolder(String folder) {
		if (folder.isEmpty()) {
			setPageComplete(false);
			return;
		}
		wstate.selectedFolder = Paths.get(folder);
		
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
