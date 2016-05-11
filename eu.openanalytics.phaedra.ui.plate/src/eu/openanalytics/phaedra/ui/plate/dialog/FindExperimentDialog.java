package eu.openanalytics.phaedra.ui.plate.dialog;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import eu.openanalytics.phaedra.base.ui.util.folderbrowser.FolderBrowserFactory;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentContentProvider;
import eu.openanalytics.phaedra.ui.plate.util.ExperimentLabelProvider;

public class FindExperimentDialog extends TitleAreaDialog {

	private TreeViewer viewer;
	
	private ProtocolClass pClass;
	private Protocol protocol;
	
	private Experiment selectedExperiment;
	
	public FindExperimentDialog(Shell parentShell) {
		super(parentShell);
	}

	public void setProtocolClass(ProtocolClass pClass) {
		this.pClass = pClass;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
	
	public Experiment getSelectedExperiment() {
		return selectedExperiment;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Find Experiment");
		newShell.setSize(400,450);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the main part of the dialog (Input)
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(1).applyTo(container);

		viewer = FolderBrowserFactory.createBrowser(container);
		Tree tree = viewer.getTree();
		viewer.setContentProvider(new ExperimentContentProvider());
		viewer.setLabelProvider(new ExperimentLabelProvider());
		viewer.setAutoExpandLevel(0);
		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object o = SelectionUtils.getFirstObject(viewer.getSelection(), Object.class);
				if (o instanceof Experiment) {
					selectedExperiment = (Experiment)o;
					getButton(OK).setEnabled(true);
				} else {
					getButton(OK).setEnabled(false);
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		
		if (protocol != null) {
			viewer.setInput(protocol);
			viewer.expandToLevel(1);
		} else if (pClass != null) {
			viewer.setInput(pClass);
			viewer.expandToLevel(1);
		} else {
			viewer.setInput("root");
		}
		
		setTitle("Find Experiment");
		setMessage("Select an experiment in the tree below.");

		return container;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}
}