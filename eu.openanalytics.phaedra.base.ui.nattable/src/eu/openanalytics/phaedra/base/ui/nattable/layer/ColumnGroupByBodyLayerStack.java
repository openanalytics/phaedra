package eu.openanalytics.phaedra.base.ui.nattable.layer;

import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataToClipboardCommand;
import org.eclipse.nebula.widgets.nattable.freeze.FreezeLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupExpandCollapseLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupReorderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractIndexLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.reorder.ColumnReorderLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

import eu.openanalytics.phaedra.base.ui.nattable.copy.command.LimitedCopyDataCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.selection.CachedSelectionModel;

public class ColumnGroupByBodyLayerStack extends AbstractIndexLayerTransform {

	private ColumnReorderLayer columnReorderLayer;
	private ColumnGroupReorderLayer columnGroupReorderLayer;
	private ColumnHideShowLayer columnHideShowLayer;
	private ColumnGroupExpandCollapseLayer columnGroupExpandCollapseLayer;
	private SelectionLayer selectionLayer;
	private TreeLayer treeLayer;
	private FreezeLayer freezeLayer;
	private ViewportLayer viewportLayer;

	public ColumnGroupByBodyLayerStack(IUniqueIndexLayer underlyingLayer, ColumnGroupModel columnGroupModel, ITreeRowModel<?> treeModel
			, final int groupByColumnIndex) {

		columnReorderLayer = new ColumnReorderLayer(underlyingLayer);
		columnGroupReorderLayer = new ColumnGroupReorderLayer(columnReorderLayer, columnGroupModel);
		columnHideShowLayer = new ColumnHideShowLayer(columnGroupReorderLayer);
		columnGroupExpandCollapseLayer = new ColumnGroupExpandCollapseLayer(columnHideShowLayer, columnGroupModel);
		selectionLayer = new SelectionLayer(columnGroupExpandCollapseLayer, null, true);
		selectionLayer.setSelectionModel(new CachedSelectionModel(selectionLayer));
		if (treeModel == null) {
			treeLayer = null;
			freezeLayer = new FreezeLayer(selectionLayer);
			viewportLayer = new ViewportLayer(selectionLayer);
		} else {
			// Use the default Painter but with no Leaf Image to prevent indentation when there are no groups.
			treeLayer = new TreeLayer(selectionLayer, treeModel);

			freezeLayer = new FreezeLayer(treeLayer);
			viewportLayer = new ViewportLayer(treeLayer);
		}

		setUnderlyingLayer(viewportLayer);

		// Replace original CopyDataCommandHandler with a limited one.
		selectionLayer.unregisterCommandHandler(CopyDataToClipboardCommand.class);
		selectionLayer.registerCommandHandler(new LimitedCopyDataCommandHandler(selectionLayer));
	}

	public ColumnReorderLayer getColumnReorderLayer() {
		return columnReorderLayer;
	}

	public ColumnGroupReorderLayer getColumnGroupReorderLayer() {
		return columnGroupReorderLayer;
	}

	public ColumnHideShowLayer getColumnHideShowLayer() {
		return columnHideShowLayer;
	}

	public ColumnGroupExpandCollapseLayer getColumnGroupExpandCollapseLayer() {
		return columnGroupExpandCollapseLayer;
	}

	public SelectionLayer getSelectionLayer() {
		return selectionLayer;
	}

	/**
	 * Returns the TreeLayer if any (can be 'null')
	 * @return The TreeLayer or null
	 */
	public TreeLayer getTreeLayer() {
		return treeLayer;
	}

	public FreezeLayer getFreezeLayer() {
		return freezeLayer;
	}

	public ViewportLayer getViewportLayer() {
		return viewportLayer;
	}

}