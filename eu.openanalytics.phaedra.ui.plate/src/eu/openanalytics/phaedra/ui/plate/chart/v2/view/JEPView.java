package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

import eu.openanalytics.phaedra.base.datatype.util.DataUnitSupport;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.BaseDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.CompositeChartLegendView;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SerializationUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.JEPAwareDataProvider;
import eu.openanalytics.phaedra.ui.plate.classification.BaseClassificationSupport;

public abstract class JEPView<ENTITY, ITEM> extends CompositeChartLegendView<ENTITY, ITEM> {
	
	private DataUnitSupport dataUnitSupport;
	
	private BaseClassificationSupport<?> classificationSupport;

	protected abstract BaseClassificationSupport<?> createClassificationSupport();

	public JEPView() {
		Supplier<Protocol> protocolSupplier = getProtocolSupplier();
		Supplier<Properties> propertySaver = () -> getProperties();
		Consumer<Properties> propertyLoader = properties -> setProperties(properties);
		addDecorator(new SettingsDecorator(protocolSupplier, propertySaver, propertyLoader));
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		this.dataUnitSupport = new DataUnitSupport(this::reloadData);
		
		super.createPartControl(parent);
	}
	
	protected DataUnitSupport getDataUnitSupport() {
		return this.dataUnitSupport;
	}
	
	protected abstract void reloadData();
	
	@Override
	protected void addSpecificToolbarButtons(ToolBar parent) {
		super.addSpecificToolbarButtons(parent);

		// Edit classifications
		classificationSupport = createClassificationSupport();
		classificationSupport.createToolbarButton(parent);
		getSite().getPage().removeSelectionListener(classificationSupport);
		getSite().getPage().addSelectionListener(classificationSupport);
		SelectionUtils.triggerActiveSelection(classificationSupport);
	}

	@Override
	public void dispose() {
		if (dataUnitSupport != null) dataUnitSupport.dispose();
		if (classificationSupport != null) {
			getSite().getPage().removeSelectionListener(classificationSupport);
		}
		super.dispose();
	}

	@Override
	public void axisFeatureChanged(String feature, int dimension) {
		String[] jepExpressions = null;
		JEPAwareDataProvider<ENTITY, ITEM> jepDataProvider = null;
		for (AbstractChartLayer<ENTITY, ITEM> featureLayer : getChartView().getChartLayers()) {
			if (featureLayer.getDataProvider() instanceof JEPAwareDataProvider) {
				jepDataProvider = (JEPAwareDataProvider<ENTITY, ITEM>) featureLayer.getDataProvider();
				if (feature.equalsIgnoreCase(BaseDataProvider.EXPRESSIONSTRING)) {
					if (jepExpressions == null) {
						// We will only render the JEP expressions dialog screen once
						jepDataProvider.generateJEPExpression(dimension);
						jepExpressions = jepDataProvider.getJepExpressions();
					} else {
						jepDataProvider.setJepExpressions(jepExpressions);
					}
				}
			}
		}
		super.axisFeatureChanged(feature, dimension);
	}

	@Override
	public void axisFeatureChanged(AbstractChartLayer<ENTITY, ITEM> layer, String feature, int dimension) {
		if (layer.getDataProvider() instanceof JEPAwareDataProvider) {
			if (feature.equalsIgnoreCase(BaseDataProvider.EXPRESSIONSTRING)) {
				// We will only render the JEP expressions dialog screen once
				((JEPAwareDataProvider<ENTITY, ITEM>) layer.getDataProvider()).generateJEPExpression(dimension);
			}
		}
		super.axisFeatureChanged(layer, feature, dimension);
	}

	@Override
	public void axisFeatureChanged(String feature) {
		String[] jepExpressions = null;
		JEPAwareDataProvider<ENTITY, ITEM> jepDataProvider = null;
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartView().getChartLayers()) {
			if (layer.getDataProvider() instanceof JEPAwareDataProvider) {
				jepDataProvider = (JEPAwareDataProvider<ENTITY, ITEM>) layer.getDataProvider();
				if (layer.getDataProvider().getSelectedFeatures().contains(feature)) {
					jepDataProvider.removeFeature(feature);
				} else {
					layer.getDataProvider().getSelectedFeatures().add(feature);
					if (feature.equalsIgnoreCase(BaseDataProvider.EXPRESSIONSTRING)) {
						int dim = layer.getDataProvider().getSelectedFeatures().size();
						if (jepExpressions == null) {
							jepDataProvider.generateJEPExpression(dim - 1);
							jepExpressions = jepDataProvider.getJepExpressions();
						} else {
							jepDataProvider.setJepExpressions(jepExpressions);
						}
					}
					layer.getDataProvider().setDataBounds(null);
				}
				layer.getDataProvider().setFilters(null);
			}
		}
		super.axisFeatureChanged(feature);
	}

	public Properties getProperties() {
		Properties properties = new Properties();

		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartView().getChartLayers()) {
			// Selection Layer has no configuration and should always be the last added layer.
			if (layer.isSelectionLayer()) continue;

			try {
				String layerSettingsAsString = getLayerSettingsAsString(layer);

				properties.addProperty(String.valueOf(layer.getOrder()), layerSettingsAsString);
			} catch (IOException e) {
				// Skip Layer.
			}
		}

		return properties;
	}

	private String getLayerSettingsAsString(AbstractChartLayer<ENTITY, ITEM> layer) throws IOException {
		LayerSettings<ENTITY, ITEM> settings = new LayerSettings<>(layer);
		String layerSettings = SerializationUtils.toString(settings);
		return layerSettings;
	}

	protected abstract Supplier<Protocol> getProtocolSupplier();

	public void setProperties(Properties properties) {
		List<LayerSettings<ENTITY, ITEM>> allLayerSettings = getAllLayerSettings(properties);

		// No (valid) layer settings found.
		if (allLayerSettings.isEmpty()) return;

		InteractiveChartView<ENTITY, ITEM> chartView = getChartView();
		synchronized (chartView) {
			List<ITEM> entities = getEntities(chartView);

			chartView.removeAllLayers();
			for (LayerSettings<ENTITY, ITEM> layerSettings : allLayerSettings) {
				AbstractChartLayer<ENTITY, ITEM> layer = createLayer(layerSettings, entities);
				chartView.addChartLayer(layer);
				chartView.initializeLayer(layer);
			}

			// Add Selection layer
			AbstractChartLayer<ENTITY, ITEM> layer = createLayer(ChartName.SELECTION);
			chartView.addChartLayer(layer);
			chartView.initializeLayer(layer);

			// Reload only when there are entities. Otherwise, settings (e.g. features) would be reset.
			if (!entities.isEmpty()) chartView.reloadDataForAllLayers(entities);
			
			List<AbstractChartLayer<ENTITY, ITEM>> layers = chartView.getChartLayers();
			BaseLegendView<ENTITY, ITEM> legendView = getLegendView();
			legendView.setChartLayers(layers);
			legendView.getLayerOrderChangedObservable().valueChanged();
			legendView.updateLayers();
		}
	}

	private List<ITEM> getEntities(InteractiveChartView<ENTITY, ITEM> chartView) {
		for (AbstractChartLayer<ENTITY, ITEM> layer : chartView.getChartLayers()) {
			if (layer.isDataLayer()) {
				return layer.getDataProvider().getCurrentItems();
			}
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	private List<LayerSettings<ENTITY, ITEM>> getAllLayerSettings(Properties properties) {
		List<LayerSettings<ENTITY, ITEM>> allLayerSettings = new ArrayList<>();
		for (int i = 1; ; i++) {
			Object layerSettingsObject = properties.getProperty(String.valueOf(i));

			// No settings found, assume this was the last layer.
			if (layerSettingsObject == null) break;

			try {
				String layerSettingsAsString = String.valueOf(layerSettingsObject);
				LayerSettings<ENTITY, ITEM> layerSettings = (LayerSettings<ENTITY, ITEM>) SerializationUtils.fromString(layerSettingsAsString);
				allLayerSettings.add(layerSettings);
			} catch (ClassNotFoundException | IOException e) {
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			}
		}
		return allLayerSettings;
	}

}
