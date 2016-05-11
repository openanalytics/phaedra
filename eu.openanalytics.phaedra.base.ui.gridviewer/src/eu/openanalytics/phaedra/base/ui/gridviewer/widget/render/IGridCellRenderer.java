package eu.openanalytics.phaedra.base.ui.gridviewer.widget.render;

import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;

/**
 * A renderer that is responsible for painting {@link GridCell} objects onto a {@link Grid} widget.
 * The renderer must follow the lifecycle of the grid: it should be set shortly after instantiation of the
 * grid (see {@link Grid#setCellRenderer(IGridCellRenderer)}) and not changed afterwards.
 */
public interface IGridCellRenderer {
	
	/**
	 * Performs any action that is needed just before a grid is going to be rendered.
	 * 
	 * @param grid The grid that is going to be rendered.
	 */
	public void prerender(Grid grid);

	/**
	 * Render the given {@link GridCell} at the specified position.
	 * The position is relative to the {@link Grid}.
	 */
	public void render(GridCell cell, GC gc, int x, int y, int w, int h);
	
	/**
	 * Get the tooltip text that should be shown when the mouse hovers
	 * above the given {@link GridCell}.
	 */
	public String getTooltipContribution(GridCell cell);
	
	/**
	 * Release any resources this renderer was using.
	 */
	public void dispose();
}
