package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class MoveExperimentDialog extends TitleAreaDialog {

	private ComboViewer protocolViewer;
	private ListViewer experimentViewer;

	private Label currentProtocolLbl;
	private Protocol currentProtocol;
	private List<Experiment> experiments;

	public MoveExperimentDialog(Shell parentShell, List<Experiment> experiments) {
		super(parentShell);
		this.experiments = experiments;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Move Experiment(s)");
		newShell.setSize(500,400);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		// Container of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);
		
		// Container of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Current protocol:");
		
		currentProtocolLbl = new Label(main, SWT.NONE);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("New protocol:");
		
		protocolViewer = new ComboViewer(main, SWT.READ_ONLY);
		protocolViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true,false).applyTo(protocolViewer.getControl());
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Experiments to move:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		experimentViewer = new ListViewer(main);
		experimentViewer.getList().setEnabled(false);
		experimentViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(experimentViewer.getControl());
		
		if (experiments != null && !experiments.isEmpty()) {
			currentProtocol = experiments.get(0).getProtocol();
			
			currentProtocolLbl.setText(currentProtocol.getName());
			protocolViewer.setInput(ProtocolService.getInstance().getProtocols(currentProtocol.getProtocolClass()));
			if (protocolViewer.getCombo().getItemCount() > 0) {
				protocolViewer.getCombo().select(0);
			}
			experimentViewer.setInput(experiments);
		}
		
		setMessage("Move one or more experiments to another protocol." 
				+ "\nThe protocol must have the same protocol class as the current protocol.");
		setTitle("Move Experiment(s)");
		
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Move", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		checkValidity();
	}

	private void checkValidity() {
		
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn == null) return;

		if (protocolViewer.getCombo().getItemCount() == 0) {
			btn.setEnabled(false);
			setErrorMessage("No other protocols found");
			return;
		}

		ISelection selection = protocolViewer.getSelection();
		Protocol p = SelectionUtils.getFirstObject(selection, Protocol.class);
		if (p == null) {
			btn.setEnabled(false);
			setErrorMessage("Select a destination protocol.");
			return;
		}

		setErrorMessage(null);
		btn.setEnabled(true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			ISelection selection = protocolViewer.getSelection();
			Protocol newProtocol = SelectionUtils.getFirstObject(selection, Protocol.class);
			
			if (newProtocol == null || newProtocol.equals(currentProtocol)) {
				// Do not move experiments if the parent is the current parent.
				return;
			}
			
			try {
				PlateService.getInstance().moveExperiments(experiments, newProtocol);
			} catch (Exception e) {
				MessageDialog.openError(getParentShell(), "Move Failed", e.getMessage());
			}
		}
		
		super.buttonPressed(buttonId);
	}
}
