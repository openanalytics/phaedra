package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.ArrayList;
import java.util.Collections;
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
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class MovePlatesDialog extends TitleAreaDialog {

	private ComboViewer experimentViewer;
	private ListViewer plateViewer;

	private Label currentExperimentLbl;
	private Experiment currentExperiment;
	private List<Plate> plates;

	public MovePlatesDialog(Shell parentShell, List<Plate> plates) {
		super(parentShell);
		this.plates = plates;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Move Plate(s)");
		newShell.setSize(600,400);
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
		lbl.setText("Current experiment:");
		
		currentExperimentLbl = new Label(main, SWT.NONE);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("New experiment:");
		
		experimentViewer = new ComboViewer(main, SWT.READ_ONLY);
		experimentViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true,false).applyTo(experimentViewer.getControl());
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Plates to move:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		plateViewer = new ListViewer(main);
		plateViewer.getList().setEnabled(false);
		plateViewer.setContentProvider(new ArrayContentProvider());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(plateViewer.getControl());
		
		if (plates != null && !plates.isEmpty()) {
			currentExperiment = plates.get(0).getExperiment();
			currentExperimentLbl.setText(currentExperiment.getName());
			
			List<Protocol> protocols = ProtocolService.getInstance().getProtocols(currentExperiment.getProtocol().getProtocolClass());
			List<Experiment> experiments = new ArrayList<>();
			for (Protocol p: protocols) {
				experiments.addAll(PlateService.getInstance().getExperiments(p));
			}
			Collections.sort(experiments, PlateUtils.EXPERIMENT_NAME_SORTER);
			
			experimentViewer.setInput(experiments);
			if (experimentViewer.getCombo().getItemCount() > 0) {
				experimentViewer.getCombo().select(0);
			}
			plateViewer.setInput(plates);
		}
		
		setMessage("Move one or more plates to another experiment within the same protocol class.");
		setTitle("Move Plate(s)");
		
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

		if (experimentViewer.getCombo().getItemCount() == 0) {
			btn.setEnabled(false);
			setErrorMessage("No other experiments found");
			return;
		}

		ISelection selection = experimentViewer.getSelection();
		Experiment e = SelectionUtils.getFirstObject(selection, Experiment.class);
		if (e == null) {
			btn.setEnabled(false);
			setErrorMessage("Select a destination experiment.");
			return;
		}

		setErrorMessage(null);
		btn.setEnabled(true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			ISelection selection = experimentViewer.getSelection();
			Experiment newExperiment = SelectionUtils.getFirstObject(selection, Experiment.class);
			
			if (newExperiment == null || newExperiment.equals(currentExperiment)) {
				// Do not move if the parent is the current parent.
				return;
			}
			
			try {
				PlateService.getInstance().movePlates(plates, newExperiment);
			} catch (Exception e) {
				MessageDialog.openError(getParentShell(), "Move Failed", e.getMessage());
			}
		}
		
		super.buttonPressed(buttonId);
	}
}
