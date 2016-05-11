package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.util.CellImageRenderMessage;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;

public abstract class AbstractChartRenderTask<ENTITY, ITEM> extends ConcurrentTask {

	protected List<ITEM> selection;
	private int width;
	private int height;
	private int row;
	private int col;
	private int border;
	private List<LayerSettings<ENTITY, ITEM>> configs;

	public AbstractChartRenderTask(int w, int h, int row, int col) {
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
		BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = finalImage.createGraphics();

		double[][] newBounds = null;
		List<AbstractChartLayer<ENTITY, ITEM>> layers = new ArrayList<>();
		for (LayerSettings<ENTITY, ITEM> settings : getConfigs()) {
			// Layer not enabled. Do not bother initializing.
			if (!settings.isEnabled() || settings.getChartType().contains("AXE")) continue;

			// Create layer based on LayerSettings.
			AbstractChartLayer<ENTITY, ITEM> layer = getChartLayerFactory().createLayer(settings, selection);
			IDataProvider<ENTITY, ITEM> dataProvider = layer.getDataProvider();
			// Create new GroupingStrategy so not all layers refer to the same object (would cause grouping errors).
			dataProvider.setActiveGroupingStrategy(settings.getDataProviderSettings().getGroupingStategyByClassName());

			// Initialize the bounds array here for Parallel Coordinate charts which can have more dimensions.
			if (newBounds == null) {
				newBounds = new double[dataProvider.getSelectedFeatures().size()][2];
				for (int i = 0; i < newBounds.length; i++) {
					newBounds[i][0] = Float.MAX_VALUE;
					newBounds[i][1] = -Float.MAX_VALUE;
				}
			}

			layer.getChart().setPreserveAxesSpace(false);
			layer.getChart().initializePlot(false);

			// Keep largest bounds
			double[][] tempBounds = dataProvider.getDataBounds();
			updateBounds(dataProvider, tempBounds);

			for (int i = 0; i < newBounds.length; i++) {
				newBounds[i][0] = Math.min(newBounds[i][0], tempBounds[i][0]);
				newBounds[i][1] = Math.max(newBounds[i][1], tempBounds[i][1]);
			}

			layers.add(layer);
		}

		for (AbstractChartLayer<ENTITY, ITEM> layer : layers) {
			if (layer.getChartSettings().getMiscSettings().get(BaseChartView.INDEPENDENT_LAYER) == null) {
				double[][] dataBounds = layer.getDataProvider().getDataBounds();
				for (int i = 0; i < newBounds.length; i++) dataBounds[i] = newBounds[i];
			}
			layer.getChart().dataChanged();
			layer.getChart().getPlotState().setShowAxes(false);

			BufferedImage plotImage = layer.getChart().getPlotImage(width, height);

			g2d.drawImage(plotImage, 0, 0, null);
		}

		return finalImage;
	}

	protected boolean isValidChart() {
		return true;
	}

	protected void updateBounds(IDataProvider<ENTITY, ITEM> dataProvider, double[][] dataBounds) {
		// Do nothing.
	}

	protected int getRow() {
		return row;
	}

	protected int getCol() {
		return col;
	}

	public abstract ChartLayerFactory<ENTITY, ITEM> getChartLayerFactory();

	public List<LayerSettings<ENTITY, ITEM>> getConfigs() {
		return configs;
	}

	public void setConfigs(List<LayerSettings<ENTITY, ITEM>> configs) {
		this.configs = configs;
	}

}