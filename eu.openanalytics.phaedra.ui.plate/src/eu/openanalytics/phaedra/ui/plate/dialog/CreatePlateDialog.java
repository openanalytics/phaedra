package eu.openanalytics.phaedra.ui.plate.dialog;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Strings;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class CreatePlateDialog extends TitleAreaDialog {
	private static final String DEFAULT_BARCODE = "001";
	private static final int DEFAULT_ROWS = 8;
	private static final int DEFAULT_COLUMNS = 12;
	
	private Text barcode;
	private Text sequence;
	private Text description;
	private Text rows;
	private Text columns;

	private Plate plate;
	private Plate newPlate;
	private Experiment experiment;

	public CreatePlateDialog(Shell parentShell, Experiment experiment) {
		this(parentShell, experiment, DEFAULT_BARCODE, DEFAULT_ROWS, DEFAULT_COLUMNS);
	}

	public CreatePlateDialog(Shell parentShell, Experiment experiment, String barcode, int rows, int columns) {
		super(parentShell);
		this.experiment = experiment;
		this.plate = new Plate();
		this.plate.setSequence(1);
		this.plate.setBarcode(!Strings.isNullOrEmpty(barcode) ? barcode : DEFAULT_BARCODE);		
		this.plate.setRows(rows > 0 ? rows : DEFAULT_ROWS);
		this.plate.setColumns(columns > 0 ? columns : DEFAULT_COLUMNS);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create Plate");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {

		// Container of the whole dialog box
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Barcode:");

		barcode = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(barcode);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Sequence:");
		
		sequence = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sequence);

		lbl = new Label(main, SWT.NONE);
		lbl.setText("Description:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		description = new Text(main, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(description);
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Layout:");

		Composite layoutContainer = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(layoutContainer);
		
		rows = new Text(layoutContainer, SWT.BORDER);
		rows.setMessage("Rows");
		GridDataFactory.fillDefaults().hint(50,SWT.DEFAULT).applyTo(rows);
		
		lbl = new Label(layoutContainer, SWT.NONE);
		lbl.setText("x");
		
		columns = new Text(layoutContainer, SWT.BORDER);
		columns.setMessage("Columns");
		GridDataFactory.fillDefaults().hint(50,SWT.DEFAULT).applyTo(columns);
		
		lbl = new Label(layoutContainer, SWT.NONE);
		lbl.setText("(rows x columns)");
		
		setTitle("Create Plate");
		setMessage("You can create a new empty plate by specifying the properties below.");

		createDataBinding();
		return main;
	}

	private void createDataBinding() {
		DataBindingContext ctx = new DataBindingContext();
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(barcode), PojoProperties.value("barcode").observe(plate));
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(sequence), PojoProperties.value("sequence").observe(plate));
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(description), PojoProperties.value("description").observe(plate));
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(rows), PojoProperties.value("rows").observe(plate));
		ctx.bindValue(WidgetProperties.text(SWT.Modify).observe(columns), PojoProperties.value("columns").observe(plate));
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
			newPlate = PlateService.getInstance().createPlate(
					experiment, this.plate.getRows(), this.plate.getColumns());
			newPlate.setDescription(this.plate.getDescription());
			newPlate.setSequence(this.plate.getSequence());
			newPlate.setBarcode(this.plate.getBarcode());
			PlateService.getInstance().updatePlate(newPlate);			
			close();
			return;
		}
		
		super.buttonPressed(buttonId);
	}

	public Plate getNewPlate() {
		return newPlate;
	}

}