package eu.openanalytics.phaedra.ui.link.importer.wizard.page;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import eu.openanalytics.phaedra.base.ui.util.folderbrowser.FolderBrowserFactory;
import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;


public class SelectSourcePage extends BaseStatefulWizardPage {

	private TreeViewer viewer;
	private Text selectedPathTxt;

	public SelectSourcePage() {
		super("Select Source Folder");
		setTitle("Select Source Folder");
		setDescription("Select the folder containing the plate(s) you want to import.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);

		viewer = FolderBrowserFactory.createBrowser(container);

		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setLayout(new GridLayout());
		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				File file = SelectionUtils.getFirstObject(viewer.getSelection(), File.class);
				if (file != null) {
					selectedPathTxt.setText(file.getAbsolutePath());
					setPageComplete(true);
				}
			}
		});

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Selected folder:");

		selectedPathTxt = new Text(container, SWT.BORDER);
		selectedPathTxt.setText("(none)");
		selectedPathTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(!selectedPathTxt.getText().isEmpty());
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(selectedPathTxt);

		setPageComplete(false);
	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		ImportTask importTask = ((ImportWizardState)state).task;
		
		// set the given source path in the textbox if any
		//***********************************************
		String path = importTask.sourcePath;
		if (path != null && !path.isEmpty()) {
			selectedPathTxt.setText(path);
			setPageComplete(true);
		}
	}

	@Override
	public void collectState(IWizardState state) {
		ImportTask importTask = ((ImportWizardState)state).task;
		importTask.sourcePath = selectedPathTxt.getText();
	}
}
