package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.ACTIONS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.GROUPING;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NAME;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_AVAILABLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_SELECTED_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_VISIBLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.SELECTED_FEATURES;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.OPACITY;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.SIZE;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.ui.util.misc.CustomComboBoxCellEditor;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloClassificationGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloFeatureGroupingStrategy;

public class SiloLegendView extends BaseLegendView<Silo, Silo> {

	public static final String CLASSIFICATION = "Classification";

	private String[] SUBWELL_COLUMNS = new String[] { NAME, SIZE, OPACITY, GROUPING, CLASSIFICATION,
			NUMBER_AVAILABLE_POINTS, NUMBER_SELECTED_POINTS, NUMBER_VISIBLE_POINTS, SELECTED_FEATURES, ACTIONS };

	private CustomComboBoxCellEditor classificationCellEditor;

	public SiloLegendView(Composite parent, List<AbstractChartLayer<Silo, Silo>> chartLayers,
			List<IGroupingStrategy<Silo, Silo>> groupingStrategies) {
		super(parent, chartLayers, groupingStrategies);
	}

	@Override
	public void addSpecificColumns() {
		classificationCellEditor = new CustomComboBoxCellEditor(getLegendViewer().getTree(), getColumnLabels());

		// Classification column, only visible when grouping by classification
		final TreeViewerColumn classificationCol = new TreeViewerColumn(getLegendViewer(), SWT.LEFT);
		classificationCol.getColumn().setWidth(0);
		classificationCol.getColumn().setResizable(false);
		classificationCol.getColumn().setText(CLASSIFICATION);
		classificationCol.setEditingSupport(getClassificationEditingSupport());

		getLayerGroupingChangedObservable().addObserver(new Observer() {
			@Override
			public void update(Observable arg0, Object arg1) {
				AbstractChartLayer<?, ?> layer = (AbstractChartLayer<?, ?>) arg1;
				if (layer.getDataProvider().getActiveGroupingStrategy() instanceof SiloFeatureGroupingStrategy) {
					classificationCol.getColumn().setWidth(100);
					classificationCol.getColumn().setResizable(true);;
					classificationCellEditor.setItems(getColumnLabels());
				} else if (layer.getDataProvider().getActiveGroupingStrategy() instanceof SiloClassificationGroupingStrategy) {
					classificationCol.getColumn().setWidth(100);
					classificationCol.getColumn().setResizable(true);;
					classificationCellEditor.setItems(getClassificationLabels());
				} else {
					classificationCol.getColumn().setWidth(0);
					classificationCol.getColumn().setResizable(false);
				}
			}
		});
	}

	private EditingSupport getClassificationEditingSupport() {
		return new EditingSupport(getLegendViewer()) {
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractLegend) {
					AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) element;
					IGroupingStrategy<?, ?> activeStrategy = legend.getDataProvider().getActiveGroupingStrategy();

					if (activeStrategy instanceof SiloFeatureGroupingStrategy) {
						String[] classificationLabels = getColumnLabels();
						if (classificationLabels != null) {
							SiloFeatureGroupingStrategy strategy = (SiloFeatureGroupingStrategy) activeStrategy;

							strategy.setClassificationFeature(classificationLabels[Integer.parseInt(String.valueOf(value))]);
							getLayerGroupingChangedObservable().valueChanged(legend.getLayer());
							refreshLegendTree();
						}
					}
					if (activeStrategy instanceof SiloClassificationGroupingStrategy) {
						String[] classificationLabels = getClassificationLabels();
						if (classificationLabels != null) {
							SiloClassificationGroupingStrategy strategy = (SiloClassificationGroupingStrategy) activeStrategy;

							strategy.setClassificationFeature(classificationLabels[Integer.parseInt(String.valueOf(value))]);
							getLayerGroupingChangedObservable().valueChanged(legend.getLayer());
							refreshLegendTree();
						}
					}
				}
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractLegend) {
					AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) element;
					IGroupingStrategy<?, ?> activeStrategy = legend.getDataProvider().getActiveGroupingStrategy();

					if (activeStrategy instanceof SiloFeatureGroupingStrategy) {
						String[] classificationLabels = getColumnLabels();
						if (classificationLabels != null) {
							SiloFeatureGroupingStrategy strategy = (SiloFeatureGroupingStrategy) activeStrategy;

							String currentClassification = strategy.getClassificationFeature();
							if (strategy != null && currentClassification != null) {
								for (int i = 0; i < classificationLabels.length; i++) {
									if (classificationLabels[i].equals(currentClassification)) {
										return i;
									}
								}
							}
						}
					}
					if (activeStrategy instanceof SiloClassificationGroupingStrategy) {
						String[] classificationLabels = getClassificationLabels();
						if (classificationLabels != null) {
							SiloClassificationGroupingStrategy strategy = (SiloClassificationGroupingStrategy) activeStrategy;

							String currentClassification = strategy.getClassificationFeature();
							if (strategy != null && currentClassification != null) {
								for (int i = 0; i < classificationLabels.length; i++) {
									if (classificationLabels[i].equals(currentClassification)) {
										return i;
									}
								}
							}
						}
					}
					return 0;
				}
				return null;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return classificationCellEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof AbstractLegend;
			}
		};
	}

	@Override
	public String getColumnName(int columnIndex) {
		return SUBWELL_COLUMNS[columnIndex];
	}

	public Observer getItemSelectionChangedObservable() {
		return new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (arg instanceof List) {
					classificationCellEditor.setItems(getColumnLabels());
				}
			}
		};
	}

	private String[] getColumnLabels() {
		AbstractChartLayer<Silo, Silo> layer = getFirstEnabledChartLayer();
		if (layer != null && layer.isDataLayer()) {
			List<String> features = layer.getDataProvider().getFeatures();
			if (features != null && !features.isEmpty()) {
				return features.toArray(new String[] {});
			}
		}

		return new String[] {""};
	}

	private String[] getClassificationLabels() {
		Silo silo = getFirstEnabledChartLayer().getDataProvider().getKey(0);
		ProtocolClass pClass = silo.getProtocolClass();

		List<String> classColumns = new ArrayList<>();
		if (silo.getType() == GroupType.WELL.getType()) {
			List<Feature> classFeatures = ClassificationService.getInstance().findWellClassificationFeatures(pClass);
			if (!classFeatures.isEmpty()) {
				String[] columns = getColumnLabels();
				for (String column : columns) {
					String featureColumn = column.replaceAll(" \\(\\d+\\)", "");
					Feature feature = ProtocolUtils.getFeatureByName(featureColumn, pClass);
					if (feature != null) {
						if (classFeatures.contains(feature)) {
							classColumns.add(column);
						}
					}
				}
			}
		} else {
			List<SubWellFeature> classSubWellFeatures = ClassificationService.getInstance().findSubWellClassificationFeatures(pClass);
			if (!classSubWellFeatures.isEmpty()) {
				String[] columns = getColumnLabels();
				for (String column : columns) {
					String featureColumn = column.replaceAll(" \\(\\d+\\)", "");
					SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(featureColumn, pClass);
					if (feature != null) {
						if (classSubWellFeatures.contains(feature)) {
							classColumns.add(column);
						}
					}
				}
			}
		}

		if (classColumns.isEmpty()) {
			return new String[] {""};
		} else {
			return classColumns.toArray(new String[] {});
		}
	}

}