package eu.openanalytics.phaedra.ui.link.importer.view;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.link.importer.ImportService;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.util.BaseDropTarget;

public class ImportView extends ViewPart {

	public final static int LABEL_WIDTH = 150;
	public final static int TEXTBOX_WIDTH = 300;

	private Text sourceFolderTxt;
	private DropTarget sourceFolderDropTarget;

	private Text destinationProtocolTxt;
	private Text destinationExperimentTxt;
	private DropTarget destinationProtocolDropTarget;

	private Button importPlateDataBtn;
	private Button importWellDataBtn;
	private Button importImageDataBtn;
	private Button importSubWellDataBtn;

	private Button queueButton;

	private Protocol protocol;
	private String sourcePath;

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);

		// Group 1: Source --------------------------------

		Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
		group.setText("Step 1");
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		Link link = new Link(group, SWT.NONE);
		link.setText("Select an experiment folder in the tree to the left and drag it into the field below."
				+ " (<a>What is an experiment folder?</a>)");
		final DefaultToolTip tt = new DefaultToolTip(link, SWT.NONE, true);
		tt.setText("An experiment folder is a folder containing one or more plate files or folders.\n"
				+ "A plate folder usually contains well data, image files and/or sub-well data.\n"
				+ "The exact structure that is expected of a plate folder (such as the names of the files,\n"
				+ "the type of image files, etc) depends on the protocol you select in Step 2.");
		link.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				tt.show(new Point(event.x, event.y));
			}
		}); 

		Composite c = new Composite(group, SWT.NONE);
		c.setLayout(new GridLayout(2, false));

		Label expSrcLabel = new Label(c, SWT.NONE);
		expSrcLabel.setText("Experiment to import:");
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).applyTo(expSrcLabel);

		sourceFolderTxt = new Text(c, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(TEXTBOX_WIDTH, SWT.DEFAULT).applyTo(sourceFolderTxt);

		int operations = DND.DROP_COPY | DND.DROP_DEFAULT;
		Transfer[] types = new Transfer[] {LocalSelectionTransfer.getTransfer()};
		sourceFolderDropTarget = new DropTarget(sourceFolderTxt, operations);
		sourceFolderDropTarget.setTransfer(types);
		sourceFolderDropTarget.addDropListener(new BaseDropTarget() {
			public void dropImpl(DropTargetEvent event) {
				IStructuredSelection sel = (IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				File file = SelectionUtils.getFirstObject(sel, File.class);
				if (file != null) setSourcePath(file.getAbsolutePath());
			}
		});

		// Group 2: Destination ----------------------------------

		group = new Group(container, SWT.SHADOW_ETCHED_IN);
		group.setText("Step 2");
		group.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		Label lbl = new Label(group, SWT.NONE);
		lbl.setText("Select a destination protocol in the tree to the right and drag it into the field below.");

		c = new Composite(group, SWT.NONE);
		c.setLayout(new GridLayout(2, false));

		lbl = new Label(c, SWT.NONE);
		lbl.setText("Protocol:");
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).applyTo(lbl);

		destinationProtocolTxt = new Text(c, SWT.BORDER);
		//		destinationProtocolTxt.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, true).hint(TEXTBOX_WIDTH, SWT.DEFAULT).applyTo(destinationProtocolTxt);

		destinationProtocolDropTarget = new DropTarget(destinationProtocolTxt, operations);
		destinationProtocolDropTarget.setTransfer(types);
		destinationProtocolDropTarget.addDropListener(new BaseDropTarget() {
			public void dropImpl(DropTargetEvent event) {
				IStructuredSelection sel = (IStructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof Protocol) {
					setProtocol((Protocol)o);
				}
			}
		});

		lbl = new Label(c, SWT.NONE);
		lbl.setText("New Experiment Name:");

		destinationExperimentTxt = new Text(c, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(TEXTBOX_WIDTH, SWT.DEFAULT).applyTo(destinationExperimentTxt);

		// Group 3: Import options ------------------------------------

		group = new Group(container, SWT.SHADOW_NONE);
		group.setText("Step 3");
		group.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		importPlateDataBtn = new Button(group, SWT.CHECK);
		importPlateDataBtn.setText("Import plate data");
		importPlateDataBtn.setSelection(true);
		importPlateDataBtn.setEnabled(false);
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(importPlateDataBtn);

		importWellDataBtn = new Button(group, SWT.CHECK);
		importWellDataBtn.setText("Import well data");
		importWellDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(importWellDataBtn);

		importImageDataBtn = new Button(group, SWT.CHECK);
		importImageDataBtn.setText("Import image data");
		importImageDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(importImageDataBtn);

		importSubWellDataBtn = new Button(group, SWT.CHECK);
		importSubWellDataBtn.setText("Import sub-well data");
		importSubWellDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER).applyTo(importSubWellDataBtn);

		// Group 4: Buttons ------------------------------------

		Group buttonGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		buttonGroup.setText("Actions");
		buttonGroup.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonGroup);

		queueButton = new Button(buttonGroup, IDialogConstants.OK_ID);
		queueButton.setText("Queue Import");
		queueButton.setImage(IconManager.getIconImage("cd_go.png"));
		queueButton.setToolTipText("Add an import job to the job queue");
		queueButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startImport();
			}
		});
		
		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewImportImportDestinationImportSource");
	}

	@Override
	public void setFocus() {
		sourceFolderTxt.setFocus();
	}

	private void setProtocol(Protocol p) {
		protocol = p;
		if (protocol != null) {
			destinationProtocolTxt.setText(protocol.getName());
		}
	}

	private void setSourcePath(String path) {
		sourcePath = path;
		sourceFolderTxt.setText(sourcePath);
		destinationExperimentTxt.setText(FileUtils.getName(path));
	}

	private void startImport() {

		// Validate the input.
		if (sourcePath == null || sourcePath.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "No source path",
					"Please select a source path to import from.");
			return;
		}
		if (protocol == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "No protocol selected",
					"Please select a protocol to import to.");
			return;
		}
		String newExpName = destinationExperimentTxt.getText();
		if (newExpName == null || newExpName.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "No experiment name",
					"Please enter a valid experiment name.");
			return;
		}

		// Do a security check.
		boolean access = SecurityService.getInstance().checkWithDialog(Permissions.EXPERIMENT_CREATE, protocol);
		if (!access) return;

		// Create the target experiment.
		Experiment newExp = PlateService.getInstance().createExperiment(protocol);
		newExp.setName(newExpName);
		try {
			PlateService.getInstance().updateExperiment(newExp);
		} catch (Throwable t) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error!"
					, "Failed to create Experiment.\n\n" + t.getMessage());
			return;
		}
		PlateService.getInstance().updateExperiment(newExp);

		// Submit an import job.
		ImportTask task = new ImportTask();
		String userName = SecurityService.getInstance().getCurrentUserName();
		task.userName = userName;
		task.sourcePath = sourcePath;
		task.importPlateData = importPlateDataBtn.getSelection();
		task.importWellData = importWellDataBtn.getSelection();
		task.importSubWellData = importSubWellDataBtn.getSelection();
		task.importImageData = importImageDataBtn.getSelection();
		task.targetExperiment = newExp;
		ImportService.getInstance().startJob(task);
	}
}
