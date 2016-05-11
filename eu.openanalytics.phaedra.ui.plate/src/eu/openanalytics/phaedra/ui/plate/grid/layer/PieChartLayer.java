package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.awt.Color;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Shell;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;

import eu.openanalytics.phaedra.base.ui.charting.util.CellImageRenderMessage;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;

public class PieChartLayer extends PlatesLayer {

	private PlateDataAccessor dataAccessor;
	private PieChartConfig config;
	private BaseConcurrentGridCellRenderer renderer;
	
	@Override
	public String getName() {
		return "Pie Chart";
	}

	@Override
	protected void doInitialize() {
		dataAccessor = CalculationService.getInstance().getAccessor(getPlate());
		config = new PieChartConfig();
		config.loadDefaults(getPlate().getExperiment().getProtocol().getId(), getId(), getPlate());
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new PieChartRenderer();
		return renderer;
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasPlates() || !isEnabled()) return;
		update();
	}

	@Override
	public void dispose() {
		// Nothing to dispose.
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new PieChartConfigDialog(shell, getPlate(), this, config);
	}

	public void update() {
		dataAccessor = CalculationService.getInstance().getAccessor(getPlate());
		renderer.resetRendering();
		getLayerSupport().getViewer().getGrid().redraw();
	}

	private class PieChartRenderer extends BaseConcurrentGridCellRenderer {

		public PieChartRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;
			Well well = (Well)cell.getData();
			if (well == null) return;

			super.render(cell, gc, x, y, w, h);
		}

		@Override
		public void prerender(Grid grid) {
			if (!isEnabled() || !hasPlates()) return;

			super.prerender(grid);
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			if (!isEnabled() || !hasPlates()) return null;

			Well well = (Well)cell.getData();
			if (well == null) return null;

			return new PieChartImageCreator(well, w, h, cell.getRow(), cell.getColumn());
		}
	}

	private class PieChartImageCreator extends ConcurrentTask {

		private Well well;
		private int w;
		private int h;
		private int row;
		private int col;

		public PieChartImageCreator(Well well, int w, int h, int row, int col) {
			this.well = well;
			this.w = w;
			this.h = h;
			this.row = row;
			this.col = col;
		}

		@Override
		public void run() {

			DefaultPieDataset dataset = new DefaultPieDataset();
			PiePlot plot = new PiePlot(dataset);

			Plate plate = dataAccessor.getPlate();
			for (int i=0; i<PieChartConfig.MAX_PIE_FEATURES; i++) {
				Feature f = config.getFeature(config.pieFeatures[i], plate);
				if (f == null) continue;
				double value = dataAccessor.getNumericValue(well, f, f.getNormalization());
				if (Double.isNaN(value)) value = 0;
				dataset.setValue(f.getDisplayName(), value);
				plot.setSectionPaint(f.getDisplayName(), new Color(config.featureColors[i]));
			}

			plot.setLabelGenerator(null);
			plot.setShadowPaint(null);
			plot.setBackgroundPaint(null);
			plot.setOutlineVisible(false);
			JFreeChart chart = new JFreeChart(plot);
			chart.removeLegend();
			chart.setBackgroundPaint(null);

			Feature sizeFeature = config.getFeature(config.sizeFeature, plate);
			if (sizeFeature != null) {
				double value = dataAccessor.getNumericValue(well, sizeFeature, sizeFeature.getNormalization());
				double minValue = StatService.getInstance().calculate("min", plate, sizeFeature, null, sizeFeature.getNormalization());
				double maxValue = StatService.getInstance().calculate("max", plate, sizeFeature, null, sizeFeature.getNormalization());
				value = Math.min(value, maxValue);
				value = Math.max(value, minValue);
				double ratio = (value - minValue) / (maxValue - minValue);
				if (Double.isNaN(ratio) || Double.isInfinite(ratio)) ratio = 0;
				int fullSize = Math.min(w, h);
				int padding = fullSize - (int)(ratio*fullSize);
				boolean horizontalPad = w > h;
				if (horizontalPad) chart.setPadding(new RectangleInsets(0, padding/2, 0, padding/2));
				else chart.setPadding(new RectangleInsets(padding/2, 0, padding/2, 0));
			}

			setResult(new CellImageRenderMessage(this.row, this.col, chart.createBufferedImage(w, h)));
		}
	}
}
