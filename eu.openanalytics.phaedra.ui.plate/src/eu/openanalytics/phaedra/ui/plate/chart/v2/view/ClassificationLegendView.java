package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.ACTIONS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.GROUPING;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NAME;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_AVAILABLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_SELECTED_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_VISIBLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.SELECTED_FEATURES;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.OPACITY;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.SIZE;

import java.util.List;
import java.util.Observer;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.ui.util.misc.CustomComboBoxCellEditor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.AbstractClassificationGroupingStrategy;

public class ClassificationLegendView<ENTITY, ITEM> extends BaseLegendView<ENTITY, ITEM> {

	public static final String CLASSIFICATION = "Classification";

	private String[] CLASSIFICATION_COLUMNS = new String[] { NAME, SIZE, OPACITY, GROUPING, CLASSIFICATION,
			NUMBER_AVAILABLE_POINTS, NUMBER_SELECTED_POINTS, NUMBER_VISIBLE_POINTS, SELECTED_FEATURES, ACTIONS };

	private CustomComboBoxCellEditor classificationCellEditor;
	private ProtocolClass pClass;
	private AbstractClassificationGroupingStrategy<ENTITY, ITEM, ?> strategy;

	public ClassificationLegendView(Composite parent, List<AbstractChartLayer<ENTITY, ITEM>> chartLayers,
			List<IGroupingStrategy<ENTITY, ITEM>> groupingStrategies) {

		super(parent, chartLayers, groupingStrategies);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addSpecificColumns() {
		classificationCellEditor = new CustomComboBoxCellEditor(getLegendViewer().getTree(), new String[] { "" });

		// Classification column, only visible when grouping by classification
		final TreeViewerColumn classificationCol = new TreeViewerColumn(getLegendViewer(), SWT.LEFT);
		classificationCol.getColumn().setWidth(0);
		classificationCol.getColumn().setResizable(false);
		classificationCol.getColumn().setText(CLASSIFICATION);
		classificationCol.setEditingSupport(getClassificationEditingSupport());

		getLayerGroupingChangedObservable().addObserver((observable, arg) -> {
			AbstractChartLayer<ENTITY, ITEM> layer = (AbstractChartLayer<ENTITY, ITEM>) arg;
			IDataProvider<ENTITY, ITEM> dataProvider = layer.getDataProvider();
			if (dataProvider.getActiveGroupingStrategy() instanceof AbstractClassificationGroupingStrategy) {
				if (strategy != null) return;
				strategy = (AbstractClassificationGroupingStrategy<ENTITY, ITEM, ?>) dataProvider.getActiveGroupingStrategy();
				classificationCol.getColumn().setWidth(100);
				classificationCol.getColumn().setResizable(true);
				classificationCellEditor.setItems(strategy.getClassificationLabels(dataProvider));
			} else {
				strategy = null;
				classificationCol.getColumn().setWidth(0);
				classificationCol.getColumn().setResizable(false);
			}
		});
	}

	private EditingSupport getClassificationEditingSupport() {
		return new EditingSupport(getLegendViewer()) {
			@SuppressWarnings("unchecked")
			@Override
			protected void setValue(Object element, Object value) {
				if (strategy == null) return;
				if (!(element instanceof AbstractLegend) || value == null) return;
				String[] items = classificationCellEditor.getItems();
				Integer index = Integer.valueOf(String.valueOf(value));
				if (items.length == 0 || index < 0 || index >= items.length) return;
				String f = items[index];
				AbstractLegend<ENTITY, ITEM> legend = (AbstractLegend<ENTITY, ITEM>) element;
				strategy.setClassificationFeature(f, legend.getDataProvider());
				getLayerGroupingChangedObservable().valueChanged(legend.getLayer());
			}

			@Override
			protected Object getValue(Object element) {
				if (strategy == null) return null;
				if (!(element instanceof AbstractLegend)) return null;
				String selectedFeature = strategy.getClassificationFeature();
				String[] currentItems = classificationCellEditor.getItems();
				for (int i = 0; i < currentItems.length; i++) {
					if (currentItems[i].equals(selectedFeature)) return i;
				}
				return 0;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return classificationCellEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof AbstractLegend) {
					AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) element;
					return legend.getDataProvider().getActiveGroupingStrategy() instanceof AbstractClassificationGroupingStrategy;
				}
				return false;
			}
		};
	}

	@Override
	public String getColumnName(int columnIndex) {
		return CLASSIFICATION_COLUMNS[columnIndex];
	}

	public Observer getItemSelectionChangedObservable() {
		return (o, arg) -> {
			if (strategy == null) return;
			if (!(arg instanceof List)) return;
			List<?> list = (List<?>) arg;
			if (list.isEmpty()) return;
			Object object = list.get(0);
			if (!(object instanceof PlatformObject)) return;
			ProtocolClass newPClass = PlateUtils.getProtocolClass((PlatformObject) object);
			if (newPClass.equals(pClass)) return;
			strategy.setClassificationFeature(null);
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
				if (!layer.isDataLayer()) continue;
				if (layer.getDataProvider().getActiveGroupingStrategy() == strategy) {
					classificationCellEditor.setItems(strategy.getClassificationLabels(layer.getDataProvider()));
				}
			}
		};
	}

}