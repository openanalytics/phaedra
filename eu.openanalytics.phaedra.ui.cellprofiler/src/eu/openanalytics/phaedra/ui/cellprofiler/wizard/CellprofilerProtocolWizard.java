package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.nio.file.Path;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;

public class CellprofilerProtocolWizard extends BaseStatefulWizard {

	public CellprofilerProtocolWizard() {
		setWindowTitle("Cellprofiler Protocol Wizard");
		setNeedsProgressMonitor(true);
		setPreconfiguredState(new WizardState());
	}
	
	@Override
	public void addPages() {
		addPage(new SelectFolderPage());
		addPage(new SelectWellDataPage());
		addPage(new SelectImageDataPage());
	}

	@Override
	public boolean performFinish() {
		return false;
	}
	
	public static class WizardState implements IWizardState {
		public Path selectedFolder;
		
		public Path[] wellDataCandidates;
		public Path selectedWellDataFile;
		
		public String[] wellDataHeaders;
		public String[] selectedWellDataHeaders;
		
		public Path[] imageFolderCandidates;
		public Path selectedImageFolder;
	}
}
