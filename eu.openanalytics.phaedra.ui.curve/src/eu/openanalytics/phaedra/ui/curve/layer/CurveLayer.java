package eu.openanalytics.phaedra.ui.curve.layer;

import java.io.IOException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.BaseConcurrentGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.concurrent.ConcurrentTaskResult;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.threading.ConcurrentTask;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class CurveLayer extends PlatesLayer {

	private Feature currentFeature;
	private IUIEventListener featureListener;

	private CurveRenderer renderer;

	public CurveLayer() {
		featureListener = event -> {
			if (event.type == EventType.FeatureSelectionChanged) {
				currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
				if (renderer != null) renderer.resetRendering();
				getLayerSupport().getViewer().getGrid().redraw();
			}
		};
	}

	@Override
	public String getName() {
		return "Dose-response Curve";
	}

	@Override
	protected void doInitialize() {
		ProtocolUIService.getInstance().addUIEventListener(featureListener);
		currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
	}

	@Override
	public IGridCellRenderer createRenderer() {
		renderer = new CurveRenderer();
		return renderer;
	}

	@Override
	public boolean isRendering() {
		return renderer.isRendering();
	}
	
	@Override
	public void dispose() {
		ProtocolUIService.getInstance().removeUIEventListener(featureListener);
	}

	private class CurveRenderer extends BaseConcurrentGridCellRenderer {
		
		public CurveRenderer() {
			super(getLayerSupport().getViewer().getGrid());
		}
		
		@Override
		public void prerender(Grid grid) {
			if (isEnabled() && hasPlates()) super.prerender(grid);
		}
		
		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (isEnabled() && hasPlates()) super.render(cell, gc, x, y, w, h);
		}
		
		@Override
		protected ConcurrentTask createRendertask(GridCell cell, int w, int h) {
			return new CurveRenderTask((Well) cell.getData(), w, h, cell.getRow(), cell.getColumn());
		}
	}
	
	private class CurveRenderTask extends ConcurrentTask {

		private Well well;
		private int w, h;
		private int row, col;

		public CurveRenderTask(Well well, int w, int h, int row, int col) {
			this.well = well;
			this.w = w;
			this.h = h;
			this.row = row;
			this.col = col;
		}

		@Override
		public void run() {
			setResult(new ConcurrentTaskResult(row, col, null));
			
			if (well == null || currentFeature == null || well.getCompound() == null) return;

			Curve curve = CurveFitService.getInstance().getCurve(well, currentFeature);
			if (curve == null || curve.getPlot() == null) return;

			Image img = null;
			try {
				int border = Activator.getDefault().getPreferenceStore().getInt(Prefs.PADDING);
				int[] pad = calculatePadding(w, h, border);
				img = ImageUtils.addPadding(PDFToImageConverter.convert(curve.getPlot(), w-2*pad[0], h-2*pad[1]), pad[0], pad[1]);
				setResult(new ConcurrentTaskResult(row, col, img.getImageData()));
			} catch (IOException e) {
				// Will return null as result.
			} finally {
				if (img != null) img.dispose();
			}
		}
	}

}
