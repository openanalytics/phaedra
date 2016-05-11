package eu.openanalytics.phaedra.base.ui.charting.v2.chart.density;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ShaderIconCreator;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class Density2DChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private TableComboViewer comboShader;
	private Button btnSkipZeroDensity;
	private Spinner loCutSpinner;
	private Spinner hiCutSpinner;
	private Spinner gaussianSpinner;
	private Spinner transparancySpinner;
	private Combo weightCombo;
	private Combo symbolSize;

	private Shader[] allShaders;
	final List<String> features;

	public final static String[] PIXEL_SIZES = new String[] {
		"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
		"11", "12", "13", "14", "15", "16", "17", "18", "19", "20" };

	// current values
	private Shader shaderValue;
	private double loCutValue;
	private double hiCutValue;
	private float gaussValue;
	private String weightFeatureValue;
	private String pixelSizeValue;
	private float transparancyValue;
	private boolean skipZeroDensity;

	public Density2DChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();
		this.shaderValue = auxSettings.getShader();
		this.loCutValue = auxSettings.getLoCut();
		this.hiCutValue = auxSettings.getHiCut();
		List<String> auxFeatures = layer.getDataProvider().getAuxiliaryFeatures();
		if (auxFeatures != null && !auxFeatures.isEmpty()) {
			this.weightFeatureValue = auxFeatures.get(0);
		}
		this.gaussValue = auxSettings.getGauss();
		this.pixelSizeValue = String.valueOf(auxSettings.getPixelSize());
		this.transparancyValue = auxSettings.getTransparancy();
		this.allShaders = Shaders.getAllLutFileNames();
		this.features = layer.getDataProvider().getFeatures();
		this.skipZeroDensity = auxSettings.isSkipZeroDensity();
	}

	@Override
	public Control embedDialogArea(final Composite area) {
		Label lblTrans = new Label(area, SWT.NONE);
		lblTrans.setText("Transparancy: ");
		GridDataFactory.fillDefaults().applyTo(lblTrans);

		transparancySpinner = new Spinner(area, SWT.BORDER);
		transparancySpinner.setMinimum(0);
		transparancySpinner.setMaximum(100);
		transparancySpinner.setIncrement(1);
		transparancySpinner.setPageIncrement(10);
		transparancySpinner.setDigits(2);
		transparancySpinner.setSelection((int) (this.transparancyValue * 100f));
		transparancySpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double transparancy = (double) transparancySpinner.getSelection();
				getAuxiliarySettings().setTransparancy((float) (transparancy / 100));
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(transparancySpinner);

		Label lblShader = new Label(area, SWT.NONE);
		lblShader.setText("Choose a shader: ");
		GridDataFactory.fillDefaults().applyTo(lblShader);

		comboShader = new TableComboViewer(area, SWT.BORDER | SWT.READ_ONLY);
		comboShader.setContentProvider(new ArrayContentProvider());
		comboShader.setInput(allShaders);
		comboShader.getTableCombo().setShowImageWithinSelection(true);
		comboShader.setLabelProvider(new LabelProvider() {
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
		comboShader.setSelection(new StructuredSelection(shaderValue));
		comboShader.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getAuxiliarySettings().setShader(
						(Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement());
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(comboShader.getControl());

		lblShader = new Label(area, SWT.NONE);
		lblShader.setText("Skip zero density:");

		btnSkipZeroDensity = new Button(area, SWT.CHECK);
		btnSkipZeroDensity.setSelection(skipZeroDensity);
		btnSkipZeroDensity.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getAuxiliarySettings().setSkipZeroDensity(btnSkipZeroDensity.getSelection());
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().applyTo(btnSkipZeroDensity);

		Label lblLo = new Label(area, SWT.NONE);
		lblLo.setText("Lo cut:");

		loCutSpinner = new Spinner(area, SWT.BORDER);
		loCutSpinner.setMinimum(0);
		loCutSpinner.setMaximum(999);
		loCutSpinner.setIncrement(1);
		loCutSpinner.setPageIncrement(100);
		loCutSpinner.setDigits(3);
		loCutSpinner.setSelection((int) (loCutValue * 1000));
		loCutSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (loCutSpinner.getSelection() >= hiCutSpinner.getSelection()) {
					loCutSpinner.setSelection(hiCutSpinner.getSelection() - 1);
				}
				getAuxiliarySettings().setLoCut(loCutSpinner.getSelection() / 1000d);
				getLayer().dataChanged();
			};
		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(loCutSpinner);

		Label lblHi = new Label(area, SWT.NONE);
		lblHi.setText("Hi cut:");

		hiCutSpinner = new Spinner(area, SWT.BORDER);
		hiCutSpinner.setMinimum(1);
		hiCutSpinner.setMaximum(1000);
		hiCutSpinner.setIncrement(1);
		hiCutSpinner.setPageIncrement(100);
		hiCutSpinner.setDigits(3);
		hiCutSpinner.setSelection((int) (hiCutValue * 1000));
		hiCutSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (loCutSpinner.getSelection() >= hiCutSpinner.getSelection()) {
					loCutSpinner.setSelection(hiCutSpinner.getSelection() - 1);
					getAuxiliarySettings().setLoCut(loCutSpinner.getSelection() / 1000d);
				}
				getAuxiliarySettings().setHiCut(hiCutSpinner.getSelection() / 1000d);
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(hiCutSpinner);

		Label lblGaus = new Label(area, SWT.NONE);
		lblGaus.setText("Set blur value:");

		gaussianSpinner = new Spinner(area, SWT.BORDER);
		gaussianSpinner.setMinimum(4);
		gaussianSpinner.setMaximum(16);
		gaussianSpinner.setIncrement(1);
		gaussianSpinner.setPageIncrement(1);
		gaussianSpinner.setSelection(Math.round(gaussValue * 4));
		gaussianSpinner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getAuxiliarySettings().setGauss((float) gaussianSpinner.getSelection() / 4);
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gaussianSpinner);

		Label lblSize = new Label(area, SWT.NONE);
		lblSize.setText("Pixel size:");

		symbolSize = new Combo(area, SWT.READ_ONLY);
		symbolSize.setItems(PIXEL_SIZES);
		int index = Arrays.asList(PIXEL_SIZES).indexOf(pixelSizeValue);
		symbolSize.select(index);
		symbolSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getAuxiliarySettings().setPixelSize(Integer.parseInt(PIXEL_SIZES[symbolSize.getSelectionIndex()]));
				getLayer().dataChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSize);

		Label lblWeight = new Label(area, SWT.READ_ONLY);
		lblWeight.setText("Select an auxiliary axis:");

		weightCombo = new Combo(area, SWT.READ_ONLY);
		weightCombo.setItems(features.toArray(new String[features.size()]));
		weightCombo.select(features.indexOf(weightFeatureValue));
		weightCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<String> auxFeatures = new ArrayList<String>();
				auxFeatures.add(features.get(weightCombo.getSelectionIndex()));
				getLayer().getDataProvider().setAuxiliaryFeatures(auxFeatures);
				getLayer().dataChanged();
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(weightCombo);
		return area;
	}

	private Image createShaderImage(Composite parent, Shader shader) {
		if (shader != null) {
			ShaderIconCreator icon = new ShaderIconCreator(shader, true, Color.BLACK, 48, 16, 4, 1);
			Image image = icon.paintIcon(parent.getDisplay(), 1, 1);
			return image;
		}
		return null;
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		AuxiliaryChartSettings auxSettings = getAuxiliarySettings();
		auxSettings.setShader(shaderValue);
		auxSettings.setLoCut(loCutValue);
		auxSettings.setHiCut(hiCutValue);
		auxSettings.setTransparancy(transparancyValue);

		// reset auxiliary feature
		List<String> auxFeatures = new ArrayList<String>();
		auxFeatures.add(weightFeatureValue);
		getLayer().getDataProvider().setAuxiliaryFeatures(auxFeatures);

		auxSettings.setGauss(gaussValue);
		auxSettings.setPixelSize(Integer.parseInt(pixelSizeValue));
		auxSettings.setSkipZeroDensity(skipZeroDensity);

		// would be better if only setting changed but topcat calculation
		// dependent
		getLayer().dataChanged();
		super.cancelPressed();
	}

	private AuxiliaryChartSettings getAuxiliarySettings() {
		return getSettings().getAuxiliaryChartSettings().get(0);
	}
}