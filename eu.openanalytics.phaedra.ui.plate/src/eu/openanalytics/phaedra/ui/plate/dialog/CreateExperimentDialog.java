package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class CreateExperimentDialog extends TitleAreaDialog {

	private ComboViewer protocolCmbViewer;
	private Text descriptionTxt;
	private Text commentTxt;
	private Text nameTxt;
	private Text creatorTxt;
	private DateTime creationDateDt;

	private List<Protocol> protocols;

	public CreateExperimentDialog(Shell parentShell, List<Protocol> protocols) {
		super(parentShell);
		this.protocols = protocols;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create Experiment");
		newShell.setSize(500,400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the main part of the dialog (Input)
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);

		Label lbl = new Label(main, SWT.NONE); 
		lbl.setText("Protocol:");

		Combo protocolCmb = new Combo(main, SWT.READ_ONLY);
		protocolCmb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		protocolCmbViewer = new ComboViewer(protocolCmb);
		protocolCmbViewer.setContentProvider(new ArrayContentProvider());
		protocolCmbViewer.setLabelProvider(new LabelProvider());
		protocolCmbViewer.setInput(protocols);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Name:");

		nameTxt = new Text(main, SWT.BORDER);
		nameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		nameTxt.setFocus();

		lbl = new Label(main, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		lbl.setText("Description:");

		descriptionTxt = new Text(main, SWT.BORDER);
		descriptionTxt.setTextLimit(200);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(descriptionTxt);

		lbl = new Label(main, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		lbl.setText("Comment:");

		commentTxt = new Text(main, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		commentTxt.setTextLimit(1600);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(commentTxt);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Creation date:");

		creationDateDt = new DateTime(main, SWT.NONE | SWT.BORDER);
		creationDateDt.setEnabled(false);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(creationDateDt);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Creator:");

		creatorTxt = new Text(main, SWT.BORDER);
		creatorTxt.setEnabled(false);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(creatorTxt);

		setTitle("Create Experiment");
		setMessage("Create a new empty experiment by specifying the properties below.");

		initExperiment();

		return main;
	}

	private void initExperiment() {
		protocolCmbViewer.setSelection(new StructuredSelection(protocols.get(0)));

		nameTxt.setText("New Experiment");
		descriptionTxt.setText("");
		commentTxt.setText("");
		creatorTxt.setText(SecurityService.getInstance().getCurrentUserName());

		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH);
		int d = cal.get(Calendar.DAY_OF_MONTH);
		creationDateDt.setDate(y, m, d);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			close();
			return;
		}

		if (buttonId == IDialogConstants.OK_ID) {
			Protocol protocol = (Protocol)((StructuredSelection)protocolCmbViewer.getSelection()).getFirstElement();
			Experiment experiment = PlateService.getInstance().createExperiment(protocol);
			experiment.setName(nameTxt.getText());
			experiment.setDescription(descriptionTxt.getText());
			experiment.setComments(commentTxt.getText());
			try {
				PlateService.getInstance().updateExperiment(experiment);
			} catch (Throwable t) {
				MessageDialog.openError(getParentShell(), "Error!", "Failed to create Experiment.\n\n" + t.getMessage());
				return;
			}
			close();
			return;
		}
		super.buttonPressed(buttonId);
	}
}