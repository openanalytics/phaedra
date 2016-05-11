package eu.openanalytics.phaedra.base.ui.charting.v2;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.util.CellImageRenderMessage;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;

public abstract class EntityRenderTask<ENTITY, ITEM> extends ConcurrentTask {

	protected List<ITEM> selection;
	private int width;
	private int height;
	private int row;
	private int col;
	private int border;
	private LayerSettings<ENTITY, ITEM> config;

	public EntityRenderTask(int w, int h, int row, int col) {
		this.selection = new ArrayList<ITEM>();
		this.width = w;
		this.height = h;
		this.row = row;
		this.col = col;
		this.border = Activator.getDefault().getPreferenceStore().getInt(Prefs.PADDING);
	}

	@Override
	public void run() {
		if (!isValidChart()) return;

		int[] padding = { (int) (width*border/100), (int) (height*border/100) };
		BufferedImage bi = getImage(width - 2 * padding[0], height - 2 * padding[1]);
		if (bi != null) {
			setResult(new CellImageRenderMessage(this.row, this.col, ImageUtils.addPadding(bi, padding[0], padding[1])));
		}
	}

	private BufferedImage getImage(int width, int height) {
		IDataProvider<ENTITY, ITEM> dataProvider = getChartDataProvider();
		dataProvider.initialize();

		AbstractChart<ENTITY, ITEM> chart = getChart();
		AbstractChartLayer<ENTITY, ITEM> chartLayer = new AbstractChartLayer<>(chart, null, dataProvider);
		chart.setPreserveAxesSpace(false);
		dataProvider.setDataCalculator(chart.getDataCalculator());
		chart.initializePlot(false);

		dataProvider.loadData(selection, chartLayer.getChart().getType().getNumberOfDimensions());

		// Set settings in config
		ChartSettings settings = new ChartSettings(getConfig().getChartSettings());
		chartLayer.setChartSettings(settings);
		IGroupingStrategy<ENTITY, ITEM> strategy = getConfig().getDataProviderSettings().getGroupingStategyByClassName();
		dataProvider.setDataProviderSettings(getConfig().getDataProviderSettings());
		dataProvider.setActiveGroupingStrategy(strategy);

		updateChartDataProvider(dataProvider);
		dataProvider.performFiltering();

		double[][] wellBounds = dataProvider.calculateDatabounds();
		updateBounds(wellBounds);
		dataProvider.setDataBounds(wellBounds);
		chartLayer.dataChanged();

		chart.getPlotState().setShowAxes(false);

		return chart.getPlotImage(width, height);
	}

	protected boolean isValidChart() {
		return true;
	}

	protected void updateBounds(double[][] wellBounds) {
		// Do nothing.
	}

	protected void updateChartDataProvider(IDataProvider<ENTITY, ITEM> dataProvider) {
		// Do nothing.
	}

	protected int getRow() {
		return row;
	}

	protected int getCol() {
		return col;
	}
	
	public abstract AbstractChart<ENTITY, ITEM> getChart();

	public abstract IDataProvider<ENTITY, ITEM> getChartDataProvider();

	public LayerSettings<ENTITY, ITEM> getConfig() {
		return config;
	}

	public void setConfig(LayerSettings<ENTITY, ITEM> config) {
		this.config = config;
	}

}