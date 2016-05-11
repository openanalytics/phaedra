package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.util.CellImageRenderMessage;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.WellSpiderDataProvider;

public class WellSpiderRenderTask extends ConcurrentTask {

	private Well well;
	private int w;
	private int h;
	private int row;
	private int col;
	private int border;
	private List<Feature> features;
	
	public WellSpiderRenderTask(Well well, int w, int h, int row, int col) {
		this.well = well;
		this.w = w;
		this.h = h;
		this.row = row;
		this.col = col;
		this.border = Activator.getDefault().getPreferenceStore().getInt(Prefs.PADDING);
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
	}
	
	@Override
	public void run() {
		int[] padding = { (int) (w*border/100), (int) (h*border/100) };
		BufferedImage bi = ImageUtils.addPadding(getImage(w-2*padding[0], h-2*padding[1]), padding[0], padding[1]);	
		setResult(new CellImageRenderMessage(this.row, this.col, bi));		
	}
	
	protected BufferedImage getImage(int w, int h) {
		
		WellSpiderDataProvider dataProvider = new WellSpiderDataProvider(well);
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		
		String[] params = new String[features.size()];
		for (int i = 0; i < features.size(); i++) {
			params[i] = features.get(i).getName();
		}
		
		for (int i=0; i<dataProvider.getSeriesCount(); i++) {
			String seriesName = dataProvider.getSeriesName(i);
			List<Well> items = dataProvider.buildSeries(i);
			if (items != null && !items.isEmpty()) {
				String[] paramsExp = new String[params.length+1];
				paramsExp[0] = seriesName;
				System.arraycopy(params, 0, paramsExp, 1, params.length);
				double[] value = dataProvider.getValue(items.get(0), paramsExp, -1);
				for (int j=0; j<params.length; j++) {
					dataset.addValue(value[j+1], seriesName, params[j]);
				}
			}
		}
		
		SpiderWebPlot plot = new SpiderWebPlot();
		plot.setDataset(dataset);
		plot.setWebFilled(true);
		plot.setStartAngle(54);
		plot.setInteriorGap(0.05);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setBackgroundAlpha(0f);
		plot.setLabelGenerator(new CategoryItemLabelGenerator() {
			@Override
			public String generateRowLabel(CategoryDataset dataset, int row) {
				return " ";
			}
			@Override
			public String generateLabel(CategoryDataset dataset, int row, int column) {
				return " ";
			}
			@Override
			public String generateColumnLabel(CategoryDataset dataset, int column) {
				return " ";
			}
		});
		
		JFreeChart chart = new JFreeChart(plot);
		chart.removeLegend();
		chart.setBackgroundPaint(null);
		
		return chart.createBufferedImage(w, h);
	}
}