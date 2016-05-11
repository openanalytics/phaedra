package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatterdensity;

import java.awt.Color;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ShaderIconCreator;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class ScatterDensityChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private Combo symbolSizeCombo;
	private Spinner loCutSpinner;
	private Spinner hiCutSpinner;
	private TableComboViewer shaderComboViewer;
	private Combo weightCombo;

	private Shader[] shaders;
	private List<String> features;

	private Job settingsChangedJob;
	private static final int FILTER_TIMEOUT = 1000;

	public final static String[] SYMBOL_SIZES = new String[] {
		"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
		"11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

	private int symbolSizeValue;
	private double loCutValue;
	private double hiCutValue;
	private Shader shaderValue;
	private String weightFeatureValue;

	public ScatterDensityChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();

		this.symbolSizeValue = auxSettings.getPixelSize();
		this.loCutValue = auxSettings.getLoCut();
		this.hiCutValue = auxSettings.getHiCut();
		this.shaders = Shaders.getAllLutFileNames();
		this.shaderValue = auxSettings.getShader();
		List<String> auxFeatures = layer.getDataProvider().getAuxiliaryFeatures();
		if (auxFeatures != null && !auxFeatures.isEmpty()) {
			this.weightFeatureValue = auxFeatures.get(0);
		}
		this.features = layer.getDataProvider().getFeatures();
	}

	@Override
	public Control embedDialogArea(final Composite area) {

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Symbol size:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);

		symbolSizeCombo = new Combo(area, SWT.READ_ONLY);
		symbolSizeCombo.setItems(SYMBOL_SIZES);
		int index = CollectionUtils.find(SYMBOL_SIZES, ""+symbolSizeValue);
		symbolSizeCombo.select(index);
		symbolSizeCombo.addListener(SWT.Selection, e -> {
			getAuxiliarySettings().setPixelSize(Integer.parseInt(SYMBOL_SIZES[symbolSizeCombo.getSelectionIndex()]));
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSizeCombo);

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
		lbl.setText("Density shader: ");
		GridDataFactory.fillDefaults().applyTo(lbl);

		shaderComboViewer = new TableComboViewer(area, SWT.BORDER | SWT.READ_ONLY);
		shaderComboViewer.setContentProvider(new ArrayContentProvider());
		shaderComboViewer.setInput(shaders);
		shaderComboViewer.getTableCombo().setShowImageWithinSelection(true);
		shaderComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				Image img = createShaderImage(area, (Shader) element);
				return (img == null) ? super.getImage(element) : img;
			}

			@Override
			public String getText(Object element) {
				return ((Shader) element).getName();
			};
		});
		shaderComboViewer.setSelection(new StructuredSelection(shaderValue));
		shaderComboViewer.addSelectionChangedListener(event -> {
			getAuxiliarySettings().setShader((Shader) ((StructuredSelection) shaderComboViewer.getSelection()).getFirstElement());
			settingsChangedJob.cancel();
			settingsChangedJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(shaderComboViewer.getControl());

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Density weight feature: ");
		GridDataFactory.fillDefaults().applyTo(lbl);

		weightCombo = new Combo(area, SWT.READ_ONLY);
		weightCombo.setItems(features.toArray(new String[features.size()]));
		weightCombo.select(features.indexOf(weightFeatureValue));
		weightCombo.addListener(SWT.Selection, e -> {
			String feature = features.get(weightCombo.getSelectionIndex());
			JobUtils.runUserJob(monitor -> {
				int numberOfDimensions = getLayer().getChart().getType().getNumberOfDimensions();
				getLayer().getDataProvider().setAuxilaryFeature(feature, numberOfDimensions, monitor);
				if (monitor.isCanceled()) return;
				getAuxiliarySettings().setWeightFeature(feature);
				getLayer().dataChanged();
			}, "Updating density weight feature", 100, getLayer().toString(), null);
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(weightCombo);

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
	protected void cancelPressed() {
		// restore all values to previous settings
		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();
		auxSettings.setLoCut(loCutValue);
		auxSettings.setHiCut(hiCutValue);
		auxSettings.setShader(shaderValue);
		getSettings().setDefaultSymbolSize(symbolSizeValue);
		getLayer().getDataProvider().setAuxiliaryFeatures(Lists.newArrayList(weightFeatureValue));

		// would be better if only setting changed but topcat calculation dependent
		getLayer().dataChanged();
		super.cancelPressed();
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

	private AuxiliaryChartSettings getAuxiliarySettings() {
		return getSettings().getAuxiliaryChartSettings().get(0);
	}

	private Image createShaderImage(Composite parent, Shader shader) {
		if (shader != null) {
			ShaderIconCreator icon = new ShaderIconCreator(shader, true, Color.BLACK, 48, 16, 4, 1);
			Image image = icon.paintIcon(parent.getDisplay(), 1, 1);
			return image;
		}
		return null;
	}
}