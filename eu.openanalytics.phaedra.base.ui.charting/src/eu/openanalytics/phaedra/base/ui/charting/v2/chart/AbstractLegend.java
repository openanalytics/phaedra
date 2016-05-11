package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.AuxilaryAxesSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public abstract class AbstractLegend<ENTITY, ITEM> {

	public static final String NAME = "Name";
	public static final String GROUPING = "Grouping";

	public static final String ACTIONS = "Actions";
	public static final String NUMBER_AVAILABLE_POINTS = "Total";
	public static final String NUMBER_SELECTED_POINTS = "Selected";
	public static final String NUMBER_VISIBLE_POINTS = "Visible";
	public static final String SELECTED_FEATURES = "Selected Features";

	private AbstractChartLayer<ENTITY, ITEM> layer;
	private boolean enabled = true;
	private List<? extends AbstractLegendItem<ENTITY, ITEM>> legendItems;
	private int order;

	public String getName() {
		return getLayer().getChart().getName().getDecription();
	}

	public abstract List<? extends AbstractLegendItem<ENTITY, ITEM>> createLegendItems();

	/* getters and setters */
	public IDataProvider<ENTITY, ITEM> getDataProvider() {
		return getLayer().getDataProvider();
	}

	public ChartSettings getChartSettings() {
		return getLayer().getChartSettings();
	}

	public AbstractChartLayer<ENTITY, ITEM> getLayer() {
		return layer;
	}

	public void setLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		this.layer = layer;
	}

	public void setLegendItems(List<? extends AbstractLegendItem<ENTITY, ITEM>> list) {
		this.legendItems = list;
	}

	public List<? extends AbstractLegendItem<ENTITY, ITEM>> getLegendItems() {
		if (legendItems == null) {
			legendItems = createLegendItems();
		}
		return legendItems;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public abstract void showSettingsDialog(Shell shell, ValueObservable observable);

	public boolean canModify(String property) {
		return ACTIONS.equals(property);
	}

	public String getPropertyValue(String property) {
		if (NAME.equals(property)) {
			return getName();
		} else if (GROUPING.equals(property)) {
			IGroupingStrategy<ENTITY, ITEM> activeGroupingStrategy = getDataProvider().getActiveGroupingStrategy();
			if (activeGroupingStrategy != null) {
				return activeGroupingStrategy.getName();
			}
		} else if (NUMBER_AVAILABLE_POINTS.equals(property)) {
			return String.valueOf(getDataProvider().getAvailableNumberOfPoints());
		} else if (NUMBER_SELECTED_POINTS.equals(property)) {
			return String.valueOf(getDataProvider().getSelectedNumberOfPoints());
		} else if (NUMBER_VISIBLE_POINTS.equals(property)) {
			return String.valueOf(getDataProvider().getVisibleNumberOfPoints());
		} else if (SELECTED_FEATURES.equals(property)) {
			String selFeature = "";
			for (String f : getDataProvider().getSelectedFeatures()) {
				if (!selFeature.isEmpty()) {
					selFeature += " | ";
				}
				selFeature += f;
			}
			return selFeature;
		}
		return "";
	}

	public void setPropertyValue(String property, String value) {
		// Do nothing.
	}

	public boolean isShowAuxilaryAxes() {
		return false;
	}

	public void showAuxilaryAxesDialog(Shell shell) {
		new AuxilaryAxesSettingsDialog<ENTITY, ITEM>(shell, getLayer()).open();
	}

	public boolean isFilterable() {
		return false;
	}

	public boolean hasAxesSupport() {
		return false;
	}
}