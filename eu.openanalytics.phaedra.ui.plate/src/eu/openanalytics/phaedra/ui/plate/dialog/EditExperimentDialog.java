package eu.openanalytics.phaedra.ui.plate.dialog;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService.MultiploMethod;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class EditExperimentDialog extends TitleAreaDialog {

	private Text descriptionTxt;
	private Text commentTxt;
	private Text nameTxt;
	private Combo multiploMethodCmb;
	private Label multiploDescriptionLbl;
	private Text multiploParamTxt;
	private Text creatorTxt;
	private DateTime creationDateDt;
	
	private Experiment experiment = null;

	public EditExperimentDialog(Shell parentShell, Experiment experiment) {
		super(parentShell);
		this.experiment = experiment;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Experiment");
		newShell.setSize(500,440);
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
		lbl.setText("Name:");

		nameTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(nameTxt);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);

		descriptionTxt = new Text(main, SWT.BORDER);
		descriptionTxt.setTextLimit(200);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(descriptionTxt);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Comment:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
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

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Multiplo method:");
		
		multiploMethodCmb = new Combo(main, SWT.READ_ONLY);
		multiploMethodCmb.setItems(Arrays.stream(MultiploMethod.values()).map(m -> m.toString()).toArray(i -> new String[i]));
		multiploMethodCmb.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				multiploDescriptionLbl.setText(MultiploMethod.get(multiploMethodCmb.getText()).getDescription());
			};
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(multiploMethodCmb);
		
		new Label(main, SWT.NONE);
		multiploDescriptionLbl = new Label(main, SWT.WRAP | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 100).applyTo(multiploDescriptionLbl);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Multiplo parameter:");
		
		multiploParamTxt = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(multiploParamTxt);
		
		setTitle("Edit Experiment");
		setMessage("You can change the properties of the experiment below.");

		initExperiment();
		
		return area;
	}

	private void initExperiment() {
		nameTxt.setText(experiment.getName());
		descriptionTxt.setText((experiment.getDescription() != null ? experiment.getDescription() : ""));
		commentTxt.setText(experiment.getComments() != null ? experiment.getComments() : "");
		creatorTxt.setText(experiment.getCreator());
		
		if (experiment.getMultiploMethod() != null) multiploMethodCmb.select(CollectionUtils.find(MultiploMethod.values(), MultiploMethod.get(experiment)));
		else multiploMethodCmb.select(0);
		if (experiment.getMultiploParameter() != null) multiploParamTxt.setText(experiment.getMultiploParameter());
		
		multiploDescriptionLbl.setText(MultiploMethod.get(multiploMethodCmb.getText()).getDescription());
		
		Date date = experiment.getCreateDate();
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
			experiment.setName(nameTxt.getText());
			experiment.setDescription(descriptionTxt.getText());
			experiment.setComments(commentTxt.getText());

			String multiploMethod = multiploMethodCmb.getText();
			if (multiploMethod.isEmpty() || multiploMethod.equals(MultiploMethod.None.toString())) multiploMethod = null;
			experiment.setMultiploMethod(multiploMethod);
			experiment.setMultiploParameter(multiploParamTxt.getText());
			
			PlateService.getInstance().updateExperiment(experiment);
			close();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
