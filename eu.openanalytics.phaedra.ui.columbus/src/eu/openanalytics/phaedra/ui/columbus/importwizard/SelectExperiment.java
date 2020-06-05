package eu.openanalytics.phaedra.ui.columbus.importwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizardPage;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.link.importer.util.IPlateMapListener;
import eu.openanalytics.phaedra.ui.link.importer.util.PlateMapper;
import eu.openanalytics.phaedra.ui.link.importer.wizard.GenericImportWizard.ImportWizardState;

public class SelectExperiment extends BaseStatefulWizardPage {

	private Button importNewExpBtn;
	private Button importNewPlatesBtn;
	private Button importExistingPlatesBtn;

	private Text newExperimentTxt;
	private Combo existingExperimentCmb;
	private Combo existingPlatesExperimentCmb;
	private PlateMapper plateMapper;

	private List<Experiment> availableExperiments;
	private String newExperimentName;

	public SelectExperiment() {
		super("Select Experiment");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(container);
		setControl(container);

		importNewExpBtn = new Button(container, SWT.RADIO);
		importNewExpBtn.setText("Import new plates into a new experiment");
		importNewExpBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectMode(1);
			}
		});

		Composite cmp = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15,0).applyTo(cmp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cmp);

		new Label(cmp, SWT.NONE).setText("Experiment name:");

		newExperimentTxt = new Text(cmp, SWT.BORDER);
		newExperimentTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				newExperimentName = newExperimentTxt.getText();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(newExperimentTxt);

		importNewPlatesBtn = new Button(container, SWT.RADIO);
		importNewPlatesBtn.setText("Import new plates into an existing experiment");
		importNewPlatesBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectMode(2);
			}
		});

		cmp = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15,0).applyTo(cmp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cmp);

		new Label(cmp, SWT.NONE).setText("Experiment:");

		existingExperimentCmb = new Combo(cmp, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(existingExperimentCmb);

		importExistingPlatesBtn = new Button(container, SWT.RADIO);
		importExistingPlatesBtn.setText("Import readings into existing plates");
		importExistingPlatesBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectMode(3);
			}
		});

		cmp = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15,0).applyTo(cmp);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cmp);

		new Label(cmp, SWT.NONE).setText("Experiment:");

		existingPlatesExperimentCmb = new Combo(cmp, SWT.READ_ONLY);
		existingPlatesExperimentCmb.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int index = existingPlatesExperimentCmb.getSelectionIndex();
				if (index != -1) {
					selectExperiment(availableExperiments.get(index));
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(existingPlatesExperimentCmb);

		plateMapper = new PlateMapper(cmp, SWT.NONE);
		plateMapper.addListener(new IPlateMapListener() {
			@Override
			public void plateMapped(PlateReading source, Plate plate) {
				setPageComplete(true);
			}
		});
		GridDataFactory.fillDefaults().grab(true,true).span(2,1).hint(SWT.DEFAULT, 300).applyTo(plateMapper);

		setTitle("Select Experiment");
		setDescription("Select the destination experiment, or create a new one."
				+ "\nYou can also add data to existing plates.");
		setControl(container);
		setPageComplete(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void applyState(IWizardState state, boolean firstTime) {
		ImportWizardState importWizardState = (ImportWizardState) state;

		newExperimentName = "New Experiment";
		String sourcePath = importWizardState.task.sourcePath;
		if (sourcePath != null && !sourcePath.isEmpty()) newExperimentName = FileUtils.getName(sourcePath);
		newExperimentTxt.setText(newExperimentName);

		// Note: needs to be initialized before experiments are selected.
		List<Meas> measurements = (List<Meas>)importWizardState.task.getParameters().get(OperaImportHelper.PARAM_MEAS_SOURCES);
		boolean hasDuplicateBarcodes = false;
		for (Meas source: measurements) {
			if (!source.isIncluded) continue;
			for (Meas other: measurements) {
				if (other.isIncluded && other != source && other.barcode.equals(source.barcode)) hasDuplicateBarcodes = true;
			}
		}
		PlateReading[] readings = new PlateReading[measurements.size()];
		for (int i=0; i<measurements.size(); i++) {
			String barcode = measurements.get(i).barcode;
			if (hasDuplicateBarcodes) barcode += "_" + measurements.get(i).name;
			readings[i] = new PlateReading();
			readings[i].setBarcode(barcode);
			readings[i].setSourcePath(measurements.get(i).source);
			readings[i].setId(10000000L + (long)(Math.random()*10000000));
		}
		plateMapper.setReadings(readings);

		Experiment targetExperiment = importWizardState.task.targetExperiment;
		Protocol protocol = (Protocol)importWizardState.task.getParameters().get(OperaImportHelper.PARAM_PROTOCOL);
		availableExperiments = new ArrayList<>(PlateService.getInstance().getOpenExperiments(protocol));
		availableExperiments.sort(PlateUtils.EXPERIMENT_NAME_SORTER);

		existingExperimentCmb.removeAll();
		existingPlatesExperimentCmb.removeAll();
		for (Experiment exp: availableExperiments) {
			existingExperimentCmb.add(exp.getName());
			existingPlatesExperimentCmb.add(exp.getName());
		}

		int selectedExp = 0;
		if (!availableExperiments.isEmpty()) {
			if (targetExperiment != null) {
				selectedExp = availableExperiments.indexOf(targetExperiment);
				if (selectedExp < 0) selectedExp = 0;
			}
			
			existingExperimentCmb.select(selectedExp);
			existingPlatesExperimentCmb.select(selectedExp);
		}

		if (firstTime) {
			if (availableExperiments.isEmpty()) {
				// If no Experiments exist, set New Experiment as default.
				importNewExpBtn.setSelection(true);
				selectMode(1);
			} else {
				// If Experiments exist, use Existing Experiment as default.
				importNewPlatesBtn.setSelection(true);
				selectMode(2);
			}
		}
		
		setPageComplete(true);
	}

	@Override
	public void collectState(IWizardState state) {
		ImportWizardState s = (ImportWizardState)state;
		s.task.plateMapping = null;
		s.task.targetExperiment = null;
		s.task.createNewPlates = true;

		if (importNewExpBtn.getSelection()) {
			s.task.getParameters().put(OperaImportHelper.PARAM_EXPERIMENT_NAME, newExperimentName);
		} else if (importNewPlatesBtn.getSelection()) {
			s.task.targetExperiment = availableExperiments.get(existingExperimentCmb.getSelectionIndex());
		} else if (importExistingPlatesBtn.getSelection()) {
			s.task.targetExperiment = availableExperiments.get(existingPlatesExperimentCmb.getSelectionIndex());
			s.task.plateMapping = plateMapper.getMapping();
			s.task.createNewPlates = false;
		}
	}

	private void selectExperiment(Experiment exp) {
		if (exp != null && plateMapper.isEnabled()) {
			List<Plate> plates = PlateService.getInstance().getPlates(exp);
			plateMapper.setPlates(plates.toArray(new Plate[plates.size()]));
			plateMapper.mapAll();
		}
	}

	private void selectMode(int mode) {
		importNewExpBtn.setSelection(mode == 1);
		importNewPlatesBtn.setSelection(mode == 2);
		importExistingPlatesBtn.setSelection(mode == 3);

		newExperimentTxt.setEnabled(mode == 1);
		existingExperimentCmb.setEnabled(mode == 2);
		existingPlatesExperimentCmb.setEnabled(mode == 3);
		plateMapper.setEnabled(mode == 3);
	}

}
