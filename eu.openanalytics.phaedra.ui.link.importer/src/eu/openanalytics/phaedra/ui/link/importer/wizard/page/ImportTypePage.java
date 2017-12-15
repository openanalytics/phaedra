package eu.openanalytics.phaedra.ui.link.importer.wizard.page;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.importer.ImportTask;
import eu.openanalytics.phaedra.ui.link.importer.Activator;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class ImportTypePage extends BaseStatefulWizardPage {

	private Button newPlatesBtn;
	private Button addToPlatesBtn;
	
	private Button importPlateDataBtn;
	private Button importWellDataBtn;
	private Button importImageDataBtn;
	private Button importSubWellDataBtn;
	
	public ImportTypePage() {
		super("Select Import Type");
		setTitle("Select Import Type");
		setDescription("You can either import data into new plates,"
				+ " or add data to existing plates.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);
		
		newPlatesBtn = new Button(container, SWT.RADIO);
		newPlatesBtn.setText("Create new plates:");
		
		Label infoLbl = new Label(container, SWT.NONE);
		infoLbl.setText("For each plate found in the source folder, a new plate will be created in Phaedra.\n"
				+ "The plate will have no layout information (well types, compounds, concentrations)\n"
				+ "until it is linked with a Plate Layout.");
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(infoLbl);
		
		Composite c = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(c);
		GridDataFactory.fillDefaults().applyTo(c);
		
//		linkPlatesBtn = new Button(c, SWT.CHECK);
//		linkPlatesBtn.setText("Link barcodes with:");
//		linkPlatesBtn.setVisible(false);
//		GridDataFactory.fillDefaults().indent(15, 0).applyTo(linkPlatesBtn);
//		
//		linkSourceCombo = new Combo(c, SWT.DROP_DOWN | SWT.READ_ONLY);
//		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(linkSourceCombo);
//		linkSourceCombo.setItems(PlateDefinitionService.getInstance().getSourceIds());
//		if (linkSourceCombo.getItemCount() > 0) {
//			linkSourceCombo.select(0);
//		}
//		linkSourceCombo.setVisible(false);
		
		addToPlatesBtn = new Button(container, SWT.RADIO);
		addToPlatesBtn.setText("Add data to existing plates:");
		
		infoLbl = new Label(container, SWT.NONE);
		infoLbl.setText("For each plate found in the source folder, you will be asked to select a destination\n"
				+ "plate in Phaedra. Any existing data will be overwritten.");
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(infoLbl);
		
		infoLbl = new Label(container, SWT.BORDER);
		infoLbl.setImage(Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/import.png").createImage());
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(infoLbl);
		
		infoLbl = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().indent(0, 10).grab(true,false).applyTo(infoLbl);
		
		infoLbl = new Label(container, SWT.NONE);
		infoLbl.setText("Select the data to import:");
		
		importPlateDataBtn = new Button(container, SWT.CHECK);
		importPlateDataBtn.setText("Import plate data");
		importPlateDataBtn.setSelection(true);
		importPlateDataBtn.setEnabled(false);
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(importPlateDataBtn);
		
		importWellDataBtn = new Button(container, SWT.CHECK);
		importWellDataBtn.setText("Import well data");
		importWellDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(importWellDataBtn);

		importImageDataBtn = new Button(container, SWT.CHECK);
		importImageDataBtn.setText("Import image data");
		importImageDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(importImageDataBtn);
		
		importSubWellDataBtn = new Button(container, SWT.CHECK);
		importSubWellDataBtn.setText("Import sub-well data");
		importSubWellDataBtn.setSelection(true);
		GridDataFactory.fillDefaults().indent(15, 0).applyTo(importSubWellDataBtn);

	}

	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		boolean createNewPlates = ((ImportWizardState)state).task.createNewPlates;
		newPlatesBtn.setSelection(createNewPlates);
		addToPlatesBtn.setSelection(!createNewPlates);
		
		boolean importPlateData = ((ImportWizardState)state).task.importPlateData;
		importPlateDataBtn.setSelection(importPlateData);
		
		boolean importWellData = ((ImportWizardState)state).task.importWellData;
		importWellDataBtn.setSelection(importWellData);

		boolean importImageData = ((ImportWizardState)state).task.importImageData;
		importImageDataBtn.setSelection(importImageData);

		boolean importSubWellData = ((ImportWizardState)state).task.importSubWellData;
		importSubWellDataBtn.setSelection(importSubWellData);
		
		setPageComplete(true);
	}
	
	@Override
	public void collectState(IWizardState state) {
		ImportTask task = ((ImportWizardState)state).task;
		task.createNewPlates = newPlatesBtn.getSelection();
		task.importPlateData = importPlateDataBtn.getSelection();
		task.importWellData = importWellDataBtn.getSelection();
		task.importImageData = importImageDataBtn.getSelection();
		task.importSubWellData = importSubWellDataBtn.getSelection();
	}
}
