package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.density.Density2DChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ShaderIconCreator;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class SubWellDensity2DConfigDialog extends SubWellChartConfigDialog {

	private AuxiliaryChartSettings auxSetting;

	private TableComboViewer comboShader;
	private Button btnSkipZeroDensity;
	private Spinner loCutSpinner;
	private Spinner hiCutSpinner;
	private Spinner gaussianSpinner;
	private Combo weightCombo;
	private Combo symbolSize;

	private Shader[] allShaders;

	// current values
	private Shader shaderValue;
	private double loCutValue;
	private double hiCutValue;
	private float gaussValue;
	private String weightFeatureValue;
	private String pixelSizeValue;
	private boolean skipZeroDensity;

	public SubWellDensity2DConfigDialog(Shell parentShell, SubWellDensity2DLayer layer) {
		super(parentShell, layer);

		List<AuxiliaryChartSettings> auxSettings = layer.getLayerSettings().getChartSettings().getAuxiliaryChartSettings();
		auxSetting = auxSettings.get(0);

		this.shaderValue = auxSetting.getShader();
		this.loCutValue = auxSetting.getLoCut();
		this.hiCutValue = auxSetting.getHiCut();
		List<String> auxFeatures = layer.getDataProvider().getAuxiliaryFeatures();
		if (auxFeatures != null && !auxFeatures.isEmpty()) {
			this.weightFeatureValue = auxFeatures.get(0);
		}
		this.gaussValue = auxSetting.getGauss();
		this.pixelSizeValue = String.valueOf(auxSetting.getPixelSize());
		this.skipZeroDensity = auxSetting.isSkipZeroDensity();

		allShaders = Shaders.getAllLutFileNames();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		createSettingsTab(getTabFolder());

		return area;
	}

	private void createSettingsTab(TabFolder tabFolder) {
		TabItem settingsTab = new TabItem(tabFolder, SWT.NONE);
		settingsTab.setText("Settings");

		final Composite settingsComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(settingsComp);

		createFillOptionCombo(settingsComp);

		Label lblShader = new Label(settingsComp, SWT.NONE);
		lblShader.setText("Choose a shader: ");

		comboShader = new TableComboViewer(settingsComp, SWT.BORDER | SWT.READ_ONLY);
		comboShader.setContentProvider(new ArrayContentProvider());
		comboShader.setInput(allShaders);
		comboShader.getTableCombo().setShowImageWithinSelection(true);
		comboShader.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				Shader shader = (Shader) element;
				if (shader != null) {
					ShaderIconCreator icon = new ShaderIconCreator(shader, true, Color.BLACK, 48, 16, 4, 1);
					return icon.paintIcon(settingsComp.getDisplay(), 1, 1);
				}
				return null;
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
				auxSetting.setShader((Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement());
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(comboShader.getControl());

		Label lblSkipZero = new Label(settingsComp, SWT.NONE);
		lblSkipZero.setText("Skip zero density:");

		btnSkipZeroDensity = new Button(settingsComp, SWT.CHECK);
		btnSkipZeroDensity.setSelection(skipZeroDensity);
		btnSkipZeroDensity.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				auxSetting.setSkipZeroDensity(btnSkipZeroDensity.getSelection());
			}
		});
		GridDataFactory.fillDefaults().applyTo(btnSkipZeroDensity);

		Label lblLo = new Label(settingsComp, SWT.NONE);
		lblLo.setText("Lo cut:");

		loCutSpinner = new Spinner(settingsComp, SWT.BORDER);
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
				auxSetting.setLoCut(loCutSpinner.getSelection() / 1000d);
			};
		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(loCutSpinner);

		Label lblHi = new Label(settingsComp, SWT.NONE);
		lblHi.setText("Hi cut:");

		hiCutSpinner = new Spinner(settingsComp, SWT.BORDER);
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
					auxSetting.setLoCut(loCutSpinner.getSelection() / 1000d);
				}
				auxSetting.setHiCut(hiCutSpinner.getSelection() / 1000d);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(hiCutSpinner);

		Label lblGaus = new Label(settingsComp, SWT.NONE);
		lblGaus.setText("Set blur value:");

		gaussianSpinner = new Spinner(settingsComp, SWT.BORDER);
		gaussianSpinner.setMinimum(4);
		gaussianSpinner.setMaximum(16);
		gaussianSpinner.setIncrement(1);
		gaussianSpinner.setPageIncrement(1);
		gaussianSpinner.setSelection(Math.round(gaussValue * 4));
		gaussianSpinner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				auxSetting.setGauss(gaussianSpinner.getSelection() / 4);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(gaussianSpinner);

		Label lblSize = new Label(settingsComp, SWT.NONE);
		lblSize.setText("Pixel size:");

		symbolSize = new Combo(settingsComp, SWT.READ_ONLY);
		symbolSize.setItems(Density2DChartSettingsDialog.PIXEL_SIZES);
		int index = Arrays.asList(Density2DChartSettingsDialog.PIXEL_SIZES).indexOf(pixelSizeValue);
		symbolSize.select(index);
		symbolSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				auxSetting.setPixelSize(Integer.parseInt(Density2DChartSettingsDialog.PIXEL_SIZES[symbolSize.getSelectionIndex()]));
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSize);

		Label lblWeight = new Label(settingsComp, SWT.READ_ONLY);
		lblWeight.setText("Select an auxiliary axis:");

		weightCombo = new Combo(settingsComp, SWT.READ_ONLY);
		weightCombo.setItems(getLayer().getDataProvider().getFeatures().toArray(new String[] {}));
		weightCombo.select(getLayer().getDataProvider().getFeatures().indexOf(weightFeatureValue));
		weightCombo.addListener(SWT.Selection, e -> {
			String feature = getLayer().getDataProvider().getFeatures().get(weightCombo.getSelectionIndex());
			getLayer().getDataProvider().setAuxilaryFeature(feature, getLayer().getDimensionCount());
			getConfig().getDataProviderSettings().setJepExpressions(getLayer().getDataProvider().getJepExpressions());
			getConfig().getDataProviderSettings().setAuxiliaryFeatures(getLayer().getDataProvider().getAuxiliaryFeatures());
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(weightCombo);

		settingsTab.setControl(settingsComp);
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		auxSetting.setShader(shaderValue);
		auxSetting.setLoCut(loCutValue);
		auxSetting.setHiCut(hiCutValue);
		// reset auxiliary feature
		List<String> auxFeatures = new ArrayList<String>();
		auxFeatures.add(weightFeatureValue);
		getLayer().getDataProvider().setAuxiliaryFeatures(auxFeatures);

		auxSetting.setGauss(gaussValue);
		auxSetting.setPixelSize(Integer.parseInt(pixelSizeValue));
		auxSetting.setSkipZeroDensity(skipZeroDensity);

		super.cancelPressed();
	}

}