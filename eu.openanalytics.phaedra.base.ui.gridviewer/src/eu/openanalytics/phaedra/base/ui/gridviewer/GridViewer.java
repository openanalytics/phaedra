package eu.openanalytics.phaedra.base.ui.gridviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.swt.IFocusService;

import eu.openanalytics.phaedra.base.ui.gridviewer.provider.AbstractGridLabelProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.IGridContentProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.provider.IGridLabelProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;

/**
 * A JFace StructuredViewer for a rectangular grid of cells, where each cell represents a distinct object.
 * This contrasts with a {@link TableViewer}, where each row represents a distinct object.
 */
public class GridViewer extends StructuredViewer {

	private Grid grid;

	public GridViewer(Composite parent) {
		this(parent, 8, 12);
	}

	public GridViewer(Composite parent, int rows, int columns) {
		grid = createGrid(parent, rows, columns);
		grid.hookSelectionListener(cells -> handleSelect(null));

		// Track focus of grid, this is required for command handlers that want to know which grid triggered the command.
		IFocusService service = (IFocusService)PlatformUI.getWorkbench().getService(IFocusService.class);
		service.addFocusTracker(grid, "grid");
		grid.addListener(SWT.Dispose, e -> {
			IFocusService s = (IFocusService)PlatformUI.getWorkbench().getService(IFocusService.class);
			s.removeFocusTracker(grid);
		});

		ContextHelper.attachContext(grid, Activator.PLUGIN_ID + ".context");
		ContextHelper.attachContext(grid, CopyItems.COPY_PASTE_CONTEXT_ID);
	}

	protected Grid createGrid(Composite parent, int rows, int columns) {
		return new Grid(parent, SWT.NONE, rows, columns);
	}
	
	@Override
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		if (labelProvider instanceof AbstractGridLabelProvider) {
			final AbstractGridLabelProvider provider = (AbstractGridLabelProvider)labelProvider;
			IGridCellRenderer cellRenderer = provider.createCellRenderer();
			grid.setCellRenderer(cellRenderer);
			grid.addListener(SWT.Resize, e -> {
				Point newSize = grid.getCurrentCellSize();
				provider.cellLayoutChanged(newSize);
			});
		}

		super.setLabelProvider(labelProvider);
	}

	@Override
	protected Widget doFindInputItem(Object element) {
		if (equals(element, getRoot())) {
			return getControl();
		}
		return null;
	}

	@Override
	protected Widget doFindItem(Object element) {
		return grid;
	}

	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		IBaseLabelProvider labelProvider = getLabelProvider();
		if (labelProvider instanceof IGridLabelProvider) {
			IGridLabelProvider gridLabelProvider = (IGridLabelProvider)labelProvider;
			if (element == null) return;
			for (int row=0; row<grid.getRows(); row++) {
				for (int col=0; col<grid.getColumns(); col++) {
					GridCell cell = grid.getCell(row, col);
					if (cell.getData() == element) {
						gridLabelProvider.update(cell, element);
						return;
					}
				}
			}
		}
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		getControl().setRedraw(false);
		try {
			preservingSelection(() -> internalRefresh(getRoot()));
		} finally {
			getControl().setRedraw(true);
		}
	}

	@Override
	protected void internalRefresh(Object element) {
		IGridContentProvider gridContentProvider = (IGridContentProvider)getContentProvider();

		int rows = gridContentProvider.getRows(getInput());
		int cols = gridContentProvider.getColumns(getInput());
		if (grid.getRows() != rows || grid.getColumns() != cols) {
			grid.resetGrid(rows, cols);
		}

		for (int row=0; row<grid.getRows(); row++) {
			for (int col=0; col<grid.getColumns(); col++) {
				Object e = gridContentProvider.getElement(row, col);
				grid.getCell(row, col).setData(e);
				updateItem(grid, e);
				if (e != null) update(e, null);
			}
		}
	}

	@Override
	public void reveal(Object element) {
		// Do nothing.
	}

	@Override
	public ISelection getSelection() {
		Control control = getControl();
		if (control == null || control.isDisposed()) return StructuredSelection.EMPTY;
		return new StructuredSelection(getSelectionFromWidget(), getComparer());
	}

	@Override
	protected List<Object> getSelectionFromWidget() {
		Set<GridCell> cells = grid.getSelectedCells();

		// Since the selected cells set order is random, sort here.
		List<GridCell> list = new ArrayList<>(cells);
		Collections.sort(list, (c1, c2) -> {
			if (c1.getRow() < c2.getRow()) return -1;
			if (c1.getRow() > c2.getRow()) return 1;
			if (c1.getColumn() < c2.getColumn()) return -1;
			if (c1.getColumn() > c2.getColumn()) return 1;
			return 0;
		});

		List<Object> selection = new ArrayList<>();
		for (GridCell cell: list) {
			Object data = cell.getData();
			if (data != null) selection.add(data);
		}
		return selection;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void setSelectionToWidget(List l, boolean reveal) {
		Set<GridCell> newSelection = new HashSet<>();
		// Convert List to Set for performance. For 12 000 rows, a Set is at least 150 times faster (350ms vs 2ms).
		if (l == null) {
			for (int row=0; row<grid.getRows(); row++) {
				for (int col=0; col<grid.getColumns(); col++) {
					GridCell cell = grid.getCell(row, col);
					newSelection.add(cell);
				}
			}
		} else {
			Set set = new HashSet<>(l);
			for (int row=0; row<grid.getRows(); row++) {
				for (int col=0; col<grid.getColumns(); col++) {
					GridCell cell = grid.getCell(row, col);
					if (set.contains(cell.getData())) {
						newSelection.add(cell);
					}
				}
			}
		}
		grid.setSelectedCells(newSelection);
	}

	@Override
	public Control getControl() {
		return grid;
	}

	public Grid getGrid() {
		return grid;
	}
}
