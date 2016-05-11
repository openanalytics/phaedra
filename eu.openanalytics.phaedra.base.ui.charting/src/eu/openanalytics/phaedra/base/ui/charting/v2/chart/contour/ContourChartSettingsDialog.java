package eu.openanalytics.phaedra.base.ui.charting.v2.chart.contour;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public class ContourChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private Spinner transparancySpinner;
	private Spinner loCutSpinner;
	private Spinner hiCutSpinner;
	private Combo pixelSizeCombo;
	private Combo weightCombo;
	private ColorSelector colorSelector;
	private Spinner levelsSpinner;
	private Spinner offsetSpinner;
	private Spinner smoothSpinner;
	private Combo levelModeCombo;

	private final List<String> features;

	private Job settingsChangedJob;
	private static final int FILTER_TIMEOUT = 1000;

	public final static String[] PIXEL_SIZES = new String[] {
		"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
		"11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

	// current values
	private float transparancyValue;
	private double loCutValue;
	private double hiCutValue;
	private String pixelSizeValue;
	private String weightFeatureValue;
	private RGB colorValue;
	private int levelsValue;
	private double offsetValue;
	private int smoothValue;
	private LevelMode levelModeValue;

	public ContourChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();

		this.transparancyValue = auxSettings.getTransparancy();
		this.loCutValue = auxSettings.getLoCut();
		this.hiCutValue = auxSettings.getHiCut();
		this.pixelSizeValue = String.valueOf(auxSettings.getPixelSize());
		List<String> auxFeatures = layer.getDataProvider().getAuxiliaryFeatures();
		if (auxFeatures != null && !auxFeatures.isEmpty()) {
			this.weightFeatureValue = auxFeatures.get(0);
		}
		this.features = layer.getDataProvider().getFeatures();

		this.colorValue = getSettings().getRGBMiscSetting(ContourPlotSettings.SETTING_COLOR);
		this.levelsValue = getSettings().getIntMiscSetting(ContourPlotSettings.SETTING_LEVELS);
		this.offsetValue = getSettings().getDoubleMiscSetting(ContourPlotSettings.SETTING_OFFSET);
		this.smoothValue = getSettings().getIntMiscSetting(ContourPlotSettings.SETTING_SMOOTH);
		this.levelModeValue = LevelMode.LOG;
		String levelModeName = getSettings().getStringMiscSetting(ContourPlotSettings.SETTING_LEVEL_MODE);
		for (LevelMode mode: LevelMode.MODES) {
			if (mode.toString().equals(levelModeName)) levelModeValue = mode;
		}
	}

	@Override
	public Control embedDialogArea(final Composite area) {

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Transparancy: ");
		GridDataFactory.fillDefaults().applyTo(lbl);

		transparancySpinner = new Spinner(area, SWT.BORDER);
		transparancySpinner.setMinimum(0);
		transparancySpinner.setMaximum(100);
		transparancySpinner.setIncrement(1);
		transparancySpinner.setPageIncrement(10);
		transparancySpinner.setDigits(2);
		transparancySpinner.setSelection((int) (this.transparancyValue * 100f));
		transparancySpinner.addListener(SWT.Selection, e -> {
			Double transparancy = (double) transparancySpinner.getSelection();
			getAuxiliarySettings().setTransparancy((float) (transparancy / 100));
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(transparancySpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Lo cut:");

		loCutSpinner = new Spinner(area, SWT.BORDER);
		loCutSpinner.setMinimum(0);
		loCutSpinner.setMaximum(999);
		loCutSpinner.setIncrement(1);
		loCutSpinner.setPageIncrement(100);
		loCutSpinner.setDigits(3);
		loCutSpinner.setSelection((int) (loCutValue * 1000));
		loCutSpinner.addListener(SWT.Selection, e -> {
			if (loCutSpinner.getSelection() >= hiCutSpinner.getSelection()) {
				loCutSpinner.setSelection(hiCutSpinner.getSelection() - 1);
			}
			getAuxiliarySettings().setLoCut(loCutSpinner.getSelection() / 1000d);
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(loCutSpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Hi cut:");

		hiCutSpinner = new Spinner(area, SWT.BORDER);
		hiCutSpinner.setMinimum(1);
		hiCutSpinner.setMaximum(1000);
		hiCutSpinner.setIncrement(1);
		hiCutSpinner.setPageIncrement(100);
		hiCutSpinner.setDigits(3);
		hiCutSpinner.setSelection((int) (hiCutValue * 1000));
		hiCutSpinner.addListener(SWT.Selection, e -> {
			if (loCutSpinner.getSelection() >= hiCutSpinner.getSelection()) {
				loCutSpinner.setSelection(hiCutSpinner.getSelection() - 1);
				getAuxiliarySettings().setLoCut(loCutSpinner.getSelection() / 1000d);
			}
			getAuxiliarySettings().setHiCut(hiCutSpinner.getSelection() / 1000d);
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(hiCutSpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Binning size:");

		pixelSizeCombo = new Combo(area, SWT.READ_ONLY);
		pixelSizeCombo.setItems(PIXEL_SIZES);
		int index = Arrays.asList(PIXEL_SIZES).indexOf(pixelSizeValue);
		pixelSizeCombo.select(index);
		pixelSizeCombo.addListener(SWT.Selection, e -> {
			getAuxiliarySettings().setPixelSize(Integer.parseInt(PIXEL_SIZES[pixelSizeCombo.getSelectionIndex()]));
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(pixelSizeCombo);

		lbl = new Label(area, SWT.READ_ONLY);
		lbl.setText("Select an auxiliary feature:");

		weightCombo = new Combo(area, SWT.READ_ONLY);
		weightCombo.setItems(features.toArray(new String[features.size()]));
		weightCombo.select(features.indexOf(weightFeatureValue));
		weightCombo.addListener(SWT.Selection, e -> {
			String feature = features.get(weightCombo.getSelectionIndex());
			JobUtils.runUserJob(monitor -> {
				int numberOfDimensions = getLayer().getChart().getType().getNumberOfDimensions();
				getLayer().getDataProvider().setAuxilaryFeature(feature, numberOfDimensions, monitor);
				getAuxiliarySettings().setWeightFeature(feature);
				getLayer().dataChanged();
			}, "Updating auxiliary feature", 100, getLayer().toString(), null);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(weightCombo);

		lbl = new Label(area, SWT.READ_ONLY);
		lbl.setText("Contour color:");

		colorSelector = new ColorSelector(area);
		colorSelector.setColorValue(colorValue);
		colorSelector.addListener(e -> {
			getSettings().setMiscSetting(ContourPlotSettings.SETTING_COLOR, colorSelector.getColorValue());
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorSelector.getButton());

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Contour levels:");

		levelsSpinner = new Spinner(area, SWT.BORDER);
		levelsSpinner.setValues(levelsValue, 1, 100, 0, 1, 5);
		levelsSpinner.addListener(SWT.Selection, e -> {
			getSettings().setMiscSetting(ContourPlotSettings.SETTING_LEVELS, levelsSpinner.getSelection());
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(levelsSpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Contour level offset:");

		offsetSpinner = new Spinner(area, SWT.BORDER);
		offsetSpinner.setValues((int)(offsetValue*1000), 0, 1000, 3, 1, 100);
		offsetSpinner.addListener(SWT.Selection, e -> {
			getSettings().setMiscSetting(ContourPlotSettings.SETTING_OFFSET, offsetSpinner.getSelection()/1000.0f);
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(offsetSpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Contour smoothing:");

		smoothSpinner = new Spinner(area, SWT.BORDER);
		smoothSpinner.setValues(smoothValue, 1, 200, 0, 1, 5);
		smoothSpinner.addListener(SWT.Selection, e -> {
			getSettings().setMiscSetting(ContourPlotSettings.SETTING_SMOOTH, smoothSpinner.getSelection());
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(smoothSpinner);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Level mode:");

		String[] levelModes = new String[LevelMode.MODES.length];
		for (int i=0; i<levelModes.length; i++) {
			levelModes[i] = LevelMode.MODES[i].toString();
		}
		levelModeCombo = new Combo(area, SWT.READ_ONLY);
		levelModeCombo.setItems(levelModes);
		levelModeCombo.select(CollectionUtils.find(LevelMode.MODES, levelModeValue));
		levelModeCombo.addListener(SWT.Selection, e -> {
			LevelMode mode = LevelMode.MODES[levelModeCombo.getSelectionIndex()];
			getSettings().setMiscSetting(ContourPlotSettings.SETTING_LEVEL_MODE, mode.toString());
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(levelModeCombo);

		this.settingsChangedJob = new Job("Applying chart settings") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(() -> getLayer().dataChanged());
				return Status.OK_STATUS;
			}
		};

		return area;
	}

	@Override
	protected void okPressed() {
		// Make sure the job runs if scheduled.
		if (settingsChangedJob.getState() == Job.SLEEPING) {
			settingsChangedJob.cancel();
			getLayer().dataChanged();
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();
		auxSettings.setTransparancy(transparancyValue);
		auxSettings.setLoCut(loCutValue);
		auxSettings.setHiCut(hiCutValue);
		auxSettings.setPixelSize(Integer.parseInt(pixelSizeValue));
		getLayer().getDataProvider().setAuxiliaryFeatures(Lists.newArrayList(weightFeatureValue));

		getSettings().setMiscSetting(ContourPlotSettings.SETTING_COLOR, colorValue);
		getSettings().setMiscSetting(ContourPlotSettings.SETTING_LEVELS, levelsValue);
		getSettings().setMiscSetting(ContourPlotSettings.SETTING_OFFSET, offsetValue);
		getSettings().setMiscSetting(ContourPlotSettings.SETTING_SMOOTH, smoothValue);
		getSettings().setMiscSetting(ContourPlotSettings.SETTING_LEVEL_MODE, levelModeValue.toString());

		// would be better if only setting changed but topcat calculation dependent
		getLayer().dataChanged();
		super.cancelPressed();
	}

	private AuxiliaryChartSettings getAuxiliarySettings() {
		return getSettings().getAuxiliaryChartSettings().get(0);
	}
}