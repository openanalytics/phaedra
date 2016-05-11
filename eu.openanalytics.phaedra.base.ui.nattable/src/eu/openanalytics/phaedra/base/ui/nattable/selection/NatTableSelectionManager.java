package eu.openanalytics.phaedra.base.ui.nattable.selection;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.event.CellSelectionEvent;
import org.eclipse.nebula.widgets.nattable.selection.event.RowSelectionEvent;

/**
 * A manager class for <code>INatTableSelectionListener</code>s.
 * These are the NatTable equivalent of SWT <code>SelectionListener</code>s, which NatTable does not have.
 * <p>
 * For the JFace version (<code>ISelectionChangedListener</code>), use <code>NatTableSelectionProvider.addSelectionChangedListener(listener)</code>.
 */
public class NatTableSelectionManager extends EventManager implements ILayerListener {

	private SelectionLayer selectionLayer;
	private IRowDataProvider<?> dataProvider;

	public NatTableSelectionManager(SelectionLayer selectionLayer, IRowDataProvider<?> dataProvider) {
		this.selectionLayer = selectionLayer;
		this.dataProvider = dataProvider;
	}

	@Override
	public void handleLayerEvent(ILayerEvent event) {
		if (event instanceof CellSelectionEvent || event instanceof RowSelectionEvent) {
			PositionCoordinate[] coords = selectionLayer.getSelectedCellPositions();
			SelectedCell[] cells = new SelectedCell[coords.length];

			for (int i = 0; i < cells.length; i++) {
				int colIndex = selectionLayer.getColumnIndexByPosition(coords[i].columnPosition);
				Object rowObject = dataProvider.getRowObject(coords[i].rowPosition);
				cells[i] = new SelectedCell(colIndex, rowObject);
			}

			for (Object l: getListeners()) {
				((INatTableSelectionListener)l).selectionChanged(cells);
			}
		}
	}

	public void addSelectionListener(INatTableSelectionListener listener) {
		addListenerObject(listener);
	}

	public void removeSelectionListener(INatTableSelectionListener listener) {
		removeListenerObject(listener);
	}

	public static interface INatTableSelectionListener {
		public void selectionChanged(SelectedCell[] selection);
	}

	public static class SelectedCell {
		public int columnIndex;
		public Object rowObject;

		public SelectedCell(int columnIndex, Object rowObject) {
			this.columnIndex = columnIndex;
			this.rowObject = rowObject;
		}
	}
}
