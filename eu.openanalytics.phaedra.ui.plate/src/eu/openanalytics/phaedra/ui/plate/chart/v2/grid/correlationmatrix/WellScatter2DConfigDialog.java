package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.BaseDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.MinMaxFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ShaderIconCreator;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.CompConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.CompGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.ConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.PlateGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellColumnGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellRowGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellStatusGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellTypeGroupingStrategy;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class WellScatter2DConfigDialog extends WellChartConfigDialog {

	private Combo groupingStrategyCmb;
	private ColorSelector colorSelector;
	private ColorSelector bgColorSelector;
	private Combo symbolSizeCmb;
	private Combo symbolTypeCmb;
	private Button bgTransparentBtn;
	private Button showLinesBtn;
	private Shader[] allShaders;

	// Original values (before modification, if any)
	private int symbolSizeValue;
	private String symbolTypeValue;
	private boolean showLinesValue;
	private Color defaultColorValue;
	private Color backgroundColorValue;
	private boolean backgroundTransparantValue;
	private String[] auxFeatureValues;
	private List<AuxiliaryChartSettings> auxSettingValues = new ArrayList<AuxiliaryChartSettings>();
	private Double[][] minMaxFiltersValue;
	private List<String> minMaxFiltersActiveItemsValue;

	// Temp values
	private AuxiliaryChartSettings[] currentAuxSettings;
	private String[] jepExpressions;

	private List<IGroupingStrategy<Plate, Well>> groupingStrategies = new ArrayList<IGroupingStrategy<Plate, Well>>() {
		private static final long serialVersionUID = -1016582051043014539L;
		{
			add(new DefaultGroupingStrategy<Plate, Well>());
			add(new PlateGroupingStrategy());
			add(new WellColumnGroupingStrategy());
			add(new WellRowGroupingStrategy());
			add(new WellStatusGroupingStrategy());
			add(new WellTypeGroupingStrategy());
			add(new CompGroupingStrategy());
			add(new CompConcGroupingStrategy());
			add(new ConcGroupingStrategy());
		}
	};

	public WellScatter2DConfigDialog(Shell parentShell, WellScatter2DLayer layer) {
		super(parentShell, layer);

		// auxilary stuff
		allShaders = Shaders.getAllLutFileNames();
		auxFeatureValues = new String[AUXILARY_DIMENSION_COUNT];
		for (int i = 0; i < auxFeatureValues.length; i++) {
			auxFeatureValues[i] = getConfig().getDataProviderSettings().getAuxiliaryFeature(i);
		}
		for (AuxiliaryChartSettings originalSetting : getSettings().getAuxiliaryChartSettings()) {
			AuxiliaryChartSettings setting = new AuxiliaryChartSettings();
			setting.setShader(originalSetting.getShader());
			setting.setWeightFeature(originalSetting.getWeightFeature());
			auxSettingValues.add(setting);
		}
		jepExpressions = getLayer().getDataProvider().getDataProviderSettings().getJepExpressions();

		// settings
		symbolSizeValue = getSettings().getDefaultSymbolSize();
		symbolTypeValue = getSettings().getDefaultSymbolType();
		showLinesValue = getSettings().isLines();
		defaultColorValue = getSettings().getDefaultColor();
		backgroundColorValue = getSettings().getBackgroundColor();
		backgroundTransparantValue = getSettings().isBackgroundTransparant();

		if (symbolTypeValue.isEmpty()) {
			symbolTypeValue = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_SYMBOL_TYPE);
		}

		// filters
		minMaxFiltersValue = new Double[layer.getDimensionCount()][2];
		for (IFilter<Plate, Well> filter : layer.getDataProvider().getFilters()) {
			if (filter instanceof MinMaxFilter) {
				MinMaxFilter<Plate, Well> minMaxFilter = (MinMaxFilter<Plate, Well>) filter;
				minMaxFiltersValue[0] = minMaxFilter.getMin();
				if (minMaxFiltersValue.length > 1) minMaxFiltersValue[1] = minMaxFilter.getMax();
				minMaxFiltersActiveItemsValue = minMaxFilter.getActiveFilterItems();
			}
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		createSettingsTab(getTabFolder());
		createGroupingTab(getTabFolder());
		createFilterTab(getTabFolder());
		createAuxiliaryTab(getLayer().getDataProvider().getFeatures(), getTabFolder());

		return area;
	}

	protected void createSettingsTab(TabFolder tabFolder) {
		TabItem settingsTab = new TabItem(tabFolder, SWT.NONE);
		settingsTab.setText("Settings");

		final Composite settingsComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(settingsComp);

		if (showFillOption()) {
			createFillOptionCombo(settingsComp);
		}

		Label lbl = new Label(settingsComp, SWT.NONE);
		lbl.setText("Symbol Size:");

		symbolSizeCmb = new Combo(settingsComp, SWT.READ_ONLY);
		symbolSizeCmb.setItems(new String[] { "0", "1", "2", "3", "4", "5" });
		symbolSizeCmb.select(getSettings().getDefaultSymbolSize());
		symbolSizeCmb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Combo) {
					Combo sourceCmb = (Combo) e.getSource();
					String size = sourceCmb.getItem(sourceCmb.getSelectionIndex());
					getSettings().setDefaultSymbolSize(Integer.parseInt(size));
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSizeCmb);

		lbl = new Label(settingsComp, SWT.NONE);
		lbl.setText("Symbol Style:");

		symbolTypeCmb = new Combo(settingsComp, SWT.READ_ONLY);
		symbolTypeCmb.setItems(Scatter2DChartSettingsDialog.SYMBOL_TYPES);
		int selected = 0;
		for (String symbol : Scatter2DChartSettingsDialog.SYMBOL_TYPES) {
			if (symbol.equalsIgnoreCase(symbolTypeValue)) {
				break;
			}
			selected++;
		}
		symbolTypeCmb.select(selected);
		symbolTypeCmb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().setDefaultSymbolType(Scatter2DChartSettingsDialog.SYMBOL_TYPES[symbolTypeCmb.getSelectionIndex()]);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolTypeCmb);

		lbl = new Label(settingsComp, SWT.NONE);
		lbl.setText("Point color:");

		colorSelector = new ColorSelector(settingsComp);
		Color defaultColor = getSettings().getDefaultColor();
		colorSelector.setColorValue(new RGB(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue()));
		colorSelector.addListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = (RGB) event.getNewValue();
				getSettings().setDefaultColor(new Color(newColor.red, newColor.green, newColor.blue));
			}
		});

		lbl = new Label(settingsComp, SWT.NONE);
		lbl.setText("Background color:");

		Color backgroundColor = getSettings().getBackgroundColor();
		bgColorSelector = new ColorSelector(settingsComp);
		bgColorSelector.setColorValue(new RGB(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor
				.getBlue()));
		bgColorSelector.addListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = (RGB) event.getNewValue();
				getSettings().setBackgroundColor(new Color(newColor.red, newColor.green, newColor.blue));
			}
		});

		bgTransparentBtn = new Button(settingsComp, SWT.CHECK);
		bgTransparentBtn.setSelection(getSettings().isBackgroundTransparant());
		bgTransparentBtn.setText("Transparent background");
		bgTransparentBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				getSettings().setBackgroundTransparant(btn.getSelection());
			}
		});

		new Label(settingsComp, SWT.NONE);

		showLinesBtn = new Button(settingsComp, SWT.CHECK);
		showLinesBtn.setSelection(showLinesValue);
		showLinesBtn.setText("Connect points");
		showLinesBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getSettings().setLines(showLinesBtn.getSelection());
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(showLinesBtn);

		settingsTab.setControl(settingsComp);
	}

	protected void createAuxiliaryTab(List<String> featureNames, TabFolder tabFolder) {
		List<Object> bufferList = new ArrayList<>();
		bufferList.add("");
		for (int i = 0; i < featureNames.size(); i++) {
			bufferList.add(featureNames.get(i));
		}
		String[] bufferArray = bufferList.toArray(new String[bufferList.size()]);

		TabItem auxTab = new TabItem(tabFolder, SWT.NONE);
		auxTab.setText("Auxiliary axes");
		final Composite auxComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(auxComp);


		currentAuxSettings = new AuxiliaryChartSettings[AUXILARY_DIMENSION_COUNT];
		for (int i = 0; i < currentAuxSettings.length; i++) {final int index = i;
			AuxiliaryChartSettings auxSettingsTemp = null;
			if (i < getSettings().getAuxiliaryChartSettings().size()) {
				auxSettingsTemp = getSettings().getAuxiliaryChartSettings().get(i);
			}
			if (auxSettingsTemp == null) {
				auxSettingsTemp = new AuxiliaryChartSettings();
				getSettings().getAuxiliaryChartSettings().add(auxSettingsTemp);
			}
			currentAuxSettings[index] = auxSettingsTemp;

			Label lbl = new Label(auxComp, SWT.NONE);
			lbl.setText("Select an auxiliary axis");

			final Combo comboFeature = new Combo(auxComp, SWT.READ_ONLY);
			final TableComboViewer comboShader = new TableComboViewer(auxComp, SWT.BORDER | SWT.READ_ONLY);

			comboFeature.setItems(bufferArray);
			comboFeature.addListener(SWT.Selection, e -> {
				String feature = comboFeature.getItem(comboFeature.getSelectionIndex());
				if (feature == null || feature.isEmpty()) {
					// Feature unselected: unselect shader also.
					comboShader.setSelection(new StructuredSelection(""));
					currentAuxSettings[index].setShader(null);
					getConfig().getDataProviderSettings().getAuxiliaryFeatures().remove(index);
				} else {
					// Make sure some shader is selected as well.
					Shader shader = (Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement();
					if (shader == null) {
						comboShader.setSelection(new StructuredSelection(allShaders[0]));
						currentAuxSettings[index].setShader(allShaders[0]);
					}
					if (feature.equalsIgnoreCase(BaseDataProvider.EXPRESSIONSTRING)) {
						getLayer().getDataProvider().setAuxilaryFeature(feature, getLayer().getDimensionCount() + index);
						jepExpressions = getLayer().getDataProvider().getDataProviderSettings().getJepExpressions();
					}
				}
				currentAuxSettings[index].setWeightFeature(feature);
				getConfig().getDataProviderSettings().getAuxiliaryFeatures().clear();
				getSettings().getAuxiliaryChartSettings().clear();
				int dim = getLayer().getDimensionCount();
				int skipped = 0;
				for (AuxiliaryChartSettings setting : currentAuxSettings) {
					if (setting.getWeightFeature() != null && !setting.getWeightFeature().isEmpty()) {
						getSettings().getAuxiliaryChartSettings().add(setting);
						getConfig().getDataProviderSettings().getAuxiliaryFeatures().add(setting.getWeightFeature());
						getConfig().getDataProviderSettings().getJepExpressions()[dim] = jepExpressions[dim + skipped];
						dim++;
					} else {
						skipped++;
					}
				}
			});

			comboShader.setContentProvider(new ArrayContentProvider());
			comboShader.setInput(allShaders);
			comboShader.getTableCombo().setShowImageWithinSelection(true);
			comboShader.setLabelProvider(new LabelProvider() {
				@Override
				public Image getImage(Object element) {
					Shader shader = (Shader) element;
					if (shader != null) {
						ShaderIconCreator icon = new ShaderIconCreator(shader, true, Color.BLACK, 48, 16, 4, 1);
						return icon.paintIcon(auxComp.getDisplay(), 1, 1);
					}
					return null;
				}

				@Override
				public String getText(Object element) {
					return ((Shader) element).getName();
				};
			});

			comboShader.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					currentAuxSettings[index].setShader((Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement());
				}
			});

			// Preselect feature and shader.
			if (currentAuxSettings[index].getWeightFeature() != null) {
				comboFeature.select(bufferList.indexOf(currentAuxSettings[index].getWeightFeature()));
				if (currentAuxSettings[index].getShader() != null) {
					comboShader.setSelection(new StructuredSelection(currentAuxSettings[index].getShader()), true);
				}
			}
		}

		auxTab.setControl(auxComp);
	}

	protected void createGroupingTab(TabFolder tabFolder) {
		TabItem groupingTab = new TabItem(tabFolder, SWT.NONE);
		groupingTab.setText("Grouping");
		Composite groupingComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(groupingComp);
		groupingTab.setControl(groupingComp);

		Label lbl = new Label(groupingComp, SWT.NONE);
		lbl.setText("Grouping mode:");

		groupingStrategyCmb = new Combo(groupingComp, SWT.READ_ONLY);

		String currentStrategy = getConfig().getDataProviderSettings().getGroupingStrategy().getName();
		int select = 0;
		int index = 0;
		for (IGroupingStrategy<Plate, Well> strat : groupingStrategies) {
			groupingStrategyCmb.add(strat.getName());
			if (currentStrategy != null && currentStrategy.equals(strat.getName())) {
				select = index;
			}
			index++;
		}
		groupingStrategyCmb.select(select);
		groupingStrategyCmb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Combo) {
					Combo sourceCmb = (Combo) e.getSource();
					getConfig().getDataProviderSettings().setGroupingStrategy(groupingStrategies.get(sourceCmb.getSelectionIndex()));
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1).applyTo(groupingStrategyCmb);
	}

	protected void createFilterTab(TabFolder tabFolder) {
		TabItem filterTab = new TabItem(tabFolder, SWT.NONE);
		filterTab.setText("Filters");
		Composite filterComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(filterComp);

		Label lblFilter = new Label(filterComp, SWT.NONE);
		lblFilter.setText("Select the filters: ");

		for (final IFilter<Plate, Well> filter : getLayer().getDataProvider().getFilters()) {
			for (final String filterItem : filter.getFilterItems()) {
				createFilterButton(filterComp, filter, filterItem);
			}
		}

		filterTab.setControl(filterComp);
	}

	protected boolean showFillOption() {
		return true;
	}

	private void createFilterButton(Composite filterComp, final IFilter<Plate, Well> filter, final String filterItem) {
		Button filterBtn = new Button(filterComp, SWT.NONE);
		filterBtn.setText(filterItem);
		filterBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				filter.doApplyFilterItem(filterItem);
			}
		});
	}

	@Override
	protected void okPressed() {
		getLayer().getDataProvider().setDataProviderSettings(getConfig().getDataProviderSettings());
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		// settings
		getSettings().setDefaultSymbolSize(symbolSizeValue);
		getSettings().setDefaultSymbolType(symbolTypeValue);
		getSettings().setDefaultColor(defaultColorValue);
		getSettings().setBackgroundColor(backgroundColorValue);
		getSettings().setBackgroundTransparant(backgroundTransparantValue);
		getSettings().setLines(showLinesValue);
		getSettings().setAuxiliaryChartSettings(auxSettingValues);

		// auxilary
		List<String> auxFeatures = new ArrayList<String>();
		for (AuxiliaryChartSettings auxSetting : auxSettingValues) {
			if (auxSetting.getWeightFeature() != null) {
				auxFeatures.add(auxSetting.getWeightFeature());
			}
		}

		getConfig().getDataProviderSettings().setAuxiliaryFeatures(auxFeatures);

		// reset filtering for dataprovider
		for (IFilter<Plate, Well> filter : getLayer().getDataProvider().getFilters()) {
			if (filter instanceof MinMaxFilter) {
				filter = new MinMaxFilter<Plate, Well>(getLayer().getDimensionCount(), getLayer().getDataProvider());
				((MinMaxFilter<Plate, Well>) filter).setActiveFilterItems(minMaxFiltersActiveItemsValue);
				((MinMaxFilter<Plate, Well>) filter).setMin(minMaxFiltersValue[0]);
				if (minMaxFiltersValue.length > 1) ((MinMaxFilter<Plate, Well>) filter).setMax(minMaxFiltersValue[1]);
			}
		}

		// reset filter for config
		getConfig().getDataProviderSettings().setFilterProperties(getLayer().getDataProvider().getDataProviderSettings().getFilterProperties());

		super.cancelPressed();
	}

}