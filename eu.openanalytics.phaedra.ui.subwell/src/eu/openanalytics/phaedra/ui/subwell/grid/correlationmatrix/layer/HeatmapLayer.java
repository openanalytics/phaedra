package eu.openanalytics.phaedra.ui.subwell.grid.correlationmatrix.layer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.ColorMethodRegistry;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.HeatmapProvider;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config.HeatmapConfig;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config.HeatmapConfigDialog;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;
import eu.openanalytics.phaedra.ui.subwell.grid.correlationmatrix.FeatureWellLayer;

public class HeatmapLayer extends FeatureWellLayer {

	private IColorMethod colorMethod;
	private IColorMethodData colorMethodData;

	private HeatmapConfig config;

	@Override
	public String getName() {
		return "Heatmap";
	}

	@Override
	protected void doInitialize() {
		if (config == null) {
			config = new HeatmapConfig();
			config.loadDefaults(getId());
		}
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new HeatmapRenderer();
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasSelection() || !isEnabled()) return;

		colorMethodData = new ColorMethodFactory.SimpleColorMethodData(new double[0], -1d, 1d, 0, -1d, 1d);
		colorMethod = ColorMethodRegistry.getInstance().getDefaultColorMethod();
		colorMethod.configure(config.getColorSettings());
		colorMethod.initialize(colorMethodData);
	}

	public IColorMethod getColorMethod() {
		return colorMethod;
	}

	private class HeatmapRenderer extends BaseGridCellRenderer {

		private ColorStore colorStore;

		public HeatmapRenderer() {
			colorStore = new ColorStore();
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasSelection() || cell.getData() == null) return;

			// Get the features
			double value = HeatmapProvider.getValue(config.getValueType(), getMatrix(), cell.getColumn(), cell.getRow());

			RGB color = new RGB(150,150,150);

			RGB lookupColor = colorMethod.getColor(value);
			if (!Double.isNaN(value) && lookupColor != null) color = lookupColor;

			Color bgColor = colorStore.get(color);
			gc.setBackground(bgColor);
			gc.fillRectangle(x,y,w,h);

			if (w > 3 && h > 3) {
				gc.setLineWidth(1);
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
				gc.drawRectangle(x,y,w,h);
			}
		}

		@Override
		public void dispose() {
			colorStore.dispose();
		}

	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new HeatmapConfigDialog(shell, this, colorMethod);
	}

	@Override
	public void setConfig(Object config) {
		this.config = (HeatmapConfig) config;
	}

	@Override
	public Object getConfig() {
		return config;
	}

}