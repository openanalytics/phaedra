package eu.openanalytics.phaedra.ui.protocol.template.simple;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.misc.FolderSelector;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;

public class SelectFolderPage extends BaseStatefulWizardPage {

	private Text protocolNameTxt;
	private FolderSelector folderSelector;
	private Button isPlateFolderBtn;
	private Button isExpFolderBtn;
	
	protected SelectFolderPage() {
		super("Select Folder");
	}

	@Override
	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(area);
		
		new Label(area, SWT.NONE).setText("Protocol Name:");
		protocolNameTxt = new Text(area, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(protocolNameTxt);

		new Label(area, SWT.NONE).setText("Sample Folder:");
		folderSelector = new FolderSelector(area, SWT.NONE);
		folderSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(!((String) e.data).isEmpty());
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(folderSelector);
		
		new Label(area, SWT.NONE);
		isPlateFolderBtn = new Button(area, SWT.RADIO);
		isPlateFolderBtn.setText("Plate folder: this folder contains data for a single plate");
		GridDataFactory.fillDefaults().indent(20, 0).applyTo(isPlateFolderBtn);
		
		new Label(area, SWT.NONE);
		isExpFolderBtn = new Button(area, SWT.RADIO);
		isExpFolderBtn.setText("Experiment folder: this folder contains data for a set of plates");
		GridDataFactory.fillDefaults().indent(20, 0).applyTo(isExpFolderBtn);
		
		setTitle("Select Folder");
    	setDescription("Select a name and a folder containing sample data.");
    	setControl(area);
    	setPageComplete(false);
	}
	
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		SimpleProtocolWizardState wstate = (SimpleProtocolWizardState) state;
		protocolNameTxt.setText(String.valueOf(wstate.parameters.get("protocol.name")));
		folderSelector.setSelectedFolder((String) wstate.parameters.get("selected.folder"));
		isPlateFolderBtn.setSelection(Boolean.valueOf((String) wstate.parameters.get("is.plate.folder")));
		isExpFolderBtn.setSelection(!Boolean.valueOf((String) wstate.parameters.get("is.plate.folder")));
	}
	
	@Override
	public void collectState(IWizardState state) {
		SimpleProtocolWizardState wstate = (SimpleProtocolWizardState) state;
		wstate.parameters.put("selected.folder", folderSelector.getSelectedFolder());
		wstate.parameters.put("is.plate.folder", String.valueOf(isPlateFolderBtn.getSelection()));
	}
}
