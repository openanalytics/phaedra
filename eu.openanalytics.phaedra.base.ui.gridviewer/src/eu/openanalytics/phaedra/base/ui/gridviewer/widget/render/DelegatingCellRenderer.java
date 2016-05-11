package eu.openanalytics.phaedra.base.ui.gridviewer.widget.render;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;

/**
 * A cell renderer that delegates the actual rendering to zero or more delegate cell renderers.
 * Note: if this renderer has zero delegates, it does nothing.
 */
public class DelegatingCellRenderer extends BaseGridCellRenderer {

	private List<IGridCellRenderer> delegates;

	public DelegatingCellRenderer() {
		delegates = new ArrayList<IGridCellRenderer>();
	}

	public void addDelegate(IGridCellRenderer renderer) {
		delegates.add(renderer);
	}

	@Override
	public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
		for (IGridCellRenderer renderer: delegates) {
			renderer.render(cell, gc, x, y, w, h);
		}
	}

	@Override
	public String getTooltipContribution(GridCell cell) {
		StringBuilder builder = new StringBuilder();
		for (IGridCellRenderer renderer: delegates) {
			String text = renderer.getTooltipContribution(cell);
			if (text != null && !text.isEmpty()) {
				if (builder.length() != 0) builder.append("\n");
				builder.append(text);
			}
		}
		return builder.toString();
	}

	@Override
	public void dispose() {
		for (IGridCellRenderer renderer: delegates) {
			renderer.dispose();
		}
	}

	@Override
	public void prerender(Grid grid) {
		for (IGridCellRenderer renderer: delegates) {
			renderer.prerender(grid);
		}
	}
}
