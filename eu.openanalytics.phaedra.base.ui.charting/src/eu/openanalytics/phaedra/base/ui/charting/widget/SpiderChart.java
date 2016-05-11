package eu.openanalytics.phaedra.base.ui.charting.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import eu.openanalytics.phaedra.base.ui.charting.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.ChartAxesMenuFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.ChartAxesMenuFactory.AxisChangedListener;
import eu.openanalytics.phaedra.base.util.misc.Properties;

public class SpiderChart<E> extends BaseCategoryChart<E> {

	private static final String SELECTED_PARAMETERS = "SELECTED_PARAMETERS";

	private ToolItem parameterDropdown;;

	private List<String> selectedFeatures;
	private Map<String, List<String>> groupedFeatures;

	public SpiderChart(Composite comp, int style) {
		super(comp, style);
	}

	public void setDataProvider(IDataProvider<E> dataProvider, boolean retainParameters) {
		this.dataProvider = dataProvider;
		if (!retainParameters) initializeParameters();
		super.setDataProvider(dataProvider);
	}

	@Override
	protected Plot createPlot(CategoryDataset dataset) {
		SpiderWebPlot plot = new SpiderWebPlot();
		plot.setDataset(dataset);
		plot.setWebFilled(true);
		plot.setStartAngle(54);
		plot.setInteriorGap(0.30);
		return plot;
	}

	@Override
	protected CategoryDataset createDataset(IDataProvider<E> dataProvider) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		String[] params = dataProvider.getParameters();
		for (int i=0; i<dataProvider.getSeriesCount(); i++) {

			String seriesName = dataProvider.getSeriesName(i);
			List<E> items = dataProvider.buildSeries(i);
			if (items != null && !items.isEmpty()) {
				String[] paramsExp = new String[params.length+1];
				paramsExp[0] = seriesName;
				System.arraycopy(params, 0, paramsExp, 1, params.length);
				double[] value = dataProvider.getValue(items.get(0), paramsExp, -1);
				for (int j=0; j<params.length; j++) {
					if (selectedFeatures.contains(params[j])) {
						dataset.addValue(value[j+1], seriesName, params[j]);
					}
				}
			}
		}
		return dataset;
	}

	public void createButtons(ToolBar parent) {
		parameterDropdown = ChartAxesMenuFactory.initializeAxisButtons(parent, 1, false)[0];
	}

	public void setSelectedFeatures(List<String> selectedFeatures) {
		this.selectedFeatures = selectedFeatures;
	}

	public List<String> getSelectedFeatures() {
		return selectedFeatures;
	}

	public Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty(SELECTED_PARAMETERS, selectedFeatures);
		return properties;
	}

	public void setProperties(Properties properties) {
		selectedFeatures = properties.getProperty(SELECTED_PARAMETERS, selectedFeatures);
		fillParameterMenus();
		buildChart();
	}

	private void initializeParameters() {
		if (dataProvider != null) {
			groupedFeatures = dataProvider.getGroupedFeatures();
			selectedFeatures = new ArrayList<>();
			if (groupedFeatures.size() > 0) {
				for (Entry<String, List<String>> e : groupedFeatures.entrySet()) {
					for (String f : e.getValue()) {
						selectedFeatures.add(f);
					}
				}

				fillParameterMenus();
			}
		}
	}

	private void fillParameterMenus() {
		if (dataProvider != null) {
			AxisChangedListener axisChangedListener = (axis, dimension) -> {
				if (selectedFeatures.contains(axis)) {
					selectedFeatures.remove(axis);
				} else {
					selectedFeatures.add(axis);
				}
				buildChart();
			};
			ChartAxesMenuFactory.updateAxisButtons(new ToolItem[] { parameterDropdown }, groupedFeatures
					, selectedFeatures, axisChangedListener );
		}
	}

}
