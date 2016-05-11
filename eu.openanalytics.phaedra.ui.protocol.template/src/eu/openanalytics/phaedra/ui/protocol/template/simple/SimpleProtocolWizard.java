package eu.openanalytics.phaedra.ui.protocol.template.simple;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class SimpleProtocolWizard extends BaseStatefulWizard {
	
	public SimpleProtocolWizard() {
		setWindowTitle("Simple Protocol Wizard");
		setNeedsProgressMonitor(true);
		setPreconfiguredState(new SimpleProtocolWizardState());
	}
	
	@Override
	public void addPages() {
		addPage(new SelectFolderPage());
		addPage(new SelectWellDataPage());
		addPage(new SelectSubWellDataPage());
		addPage(new SelectImageDataPage());
	}

	@Override
	public boolean performFinish() {
		Protocol p = SimpleProtocolAnalyzer.createProtocol((SimpleProtocolWizardState) getState());
		return (p != null);
	}

}
