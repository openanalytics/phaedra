package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.Collections;
import java.util.List;

import javax.script.ScriptException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.ConcurrentTaskResult;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.view.ScriptedChart;

public class ScriptedChartLayer extends PlatesLayer {

	private ScriptedChartRenderer renderer;
	private ScriptedChartConfig config;
	
	@Override
	public String getName() {
		return "Scripted Chart";
	}

	@Override
	protected void doInitialize() {
		config = new ScriptedChartConfig(getId());
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}
	
	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ScriptedChartConfigDialog(shell, this, config);
	}
	
	@Override
	public Object getConfig() {
		return config;
	}
	
	@Override
	public void setConfig(Object config) {
		this.config = ScriptedChartConfig.class.cast(config);
	}
	
	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new ScriptedChartRenderer();
		return renderer;
	}

	public void update() {
		renderer.resetRendering();
		getLayerSupport().getViewer().getGrid().redraw();
	}
	
	private class ScriptedChartRenderer extends BaseConcurrentGridCellRenderer {

		public ScriptedChartRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}

		@Override
		public void prerender(Grid grid) {
			if (!isEnabled() || !hasPlates()) return;
			super.prerender(grid);
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;
			Well well = (Well)cell.getData();
			if (well == null) return;
			super.render(cell, gc, x, y, w, h);
		}

		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			if (!isEnabled() || !hasPlates()) return null;
			Well well = (Well)cell.getData();
			if (well == null) return null;
			List<Well> wells = Collections.singletonList(well);
			String scriptSrc = config.scriptSrc;
			
			return new ConcurrentTask() {
				@Override
				public void run() {
					try {
						ImageData data = ScriptedChart.createChart(scriptSrc, wells, w, h);
						setResult(new ConcurrentTaskResult(cell.getRow(), cell.getColumn(), data));
					} catch (ScriptException e) {}
				}
			};
		}
	}
	
	public static class ScriptedChartConfig {
		
		private String layerId;
		private String scriptSrc;
		
		public ScriptedChartConfig(String layerId) {
			this.layerId = layerId;
			this.scriptSrc = GridState.getStringValue(GridState.ALL_PROTOCOLS, layerId, "scriptSrc");
		}
		
		public String getScriptSrc() {
			return scriptSrc;
		}
		
		public void setScriptSrc(String scriptSrc) {
			this.scriptSrc = scriptSrc;
			GridState.saveValue(GridState.ALL_PROTOCOLS, layerId, "scriptSrc", scriptSrc);
		}
		
	}
}
