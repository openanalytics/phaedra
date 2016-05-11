package eu.openanalytics.phaedra.base.ui.nattable.selection;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.layer.event.IStructuralChangeEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.StructuralDiff;
import org.eclipse.nebula.widgets.nattable.layer.event.StructuralDiff.DiffTypeEnum;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionModel;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.util.threading.JobUtils;

/**
 * <p>Tracks the selections made in the table. All selections are tracked in terms of
 * Rectangles.</p>
 *
 * <p>For example if the table has 10 rows and column 2 is selected, the
 * Rectangle tracked is (0, 2, 10, 1)</p>
 *
 * <p>Coordinates are in <i>Selection Layer positions</i></p>
 *
 * <p>When provided with an {@link ISelectionProvider}, this {@link SelectionModel}
 * will also re-apply selections after a sort/filter/... instead of clearing the selection.</p>
 *
 * @see SelectionLayer
 */
public class CachedSelectionModel extends SelectionModel {

	private Set<Range> cachedSelectedRowPositions;
	private int[] cachedSelectedColumnPositions;
	private boolean isRowCacheValid;
	private boolean isColumnCacheValid;

	private boolean clearSelectionOnChange = true;

	private ISelectionProvider selectionProvider;
	private Job reapplySelectionJob;

	public CachedSelectionModel(SelectionLayer selectionLayer) {
		super(selectionLayer);
	}

	public CachedSelectionModel(SelectionLayer selectionLayer, boolean multipleSelectionAllowed) {
		super(selectionLayer, multipleSelectionAllowed);
	}

	@Override
	public void addSelection(int columnPosition, int rowPosition) {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		super.addSelection(columnPosition, rowPosition);
	}

	@Override
	public void addSelection(Rectangle range) {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		super.addSelection(range);
	}

	public void addSelection(Rectangle range, boolean performCheck) {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		if (performCheck) super.addSelection(range);
		else super.getSelections().add(range);
	}

	@Override
	public void clearSelection() {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		super.clearSelection();
	}

	@Override
	public void clearSelection(int columnPosition, int rowPosition) {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		super.clearSelection(columnPosition, rowPosition);
	}

	@Override
	public void clearSelection(Rectangle removedSelection) {
		this.isRowCacheValid = false;
		this.isColumnCacheValid = false;
		super.clearSelection(removedSelection);
	}

	@Override
	public Set<Range> getSelectedRowPositions() {
		if (!isRowCacheValid) {
			cachedSelectedRowPositions = super.getSelectedRowPositions();
			isRowCacheValid = true;
		}
		return cachedSelectedRowPositions;
	}

	@Override
	public int[] getSelectedColumnPositions() {
		if (!isColumnCacheValid) {
			cachedSelectedColumnPositions = super.getSelectedColumnPositions();
			isColumnCacheValid = true;
		}
		return cachedSelectedColumnPositions;
	}

	/**
	 * Changes below make sure the current selection is re-applied after sorting/filtering/...
	 */

	@Override
	public void handleLayerEvent(IStructuralChangeEvent event) {
		if (this.clearSelectionOnChange) {
			if (event.isHorizontalStructureChanged()) {
				if (event.getColumnDiffs() == null) {
					Collection<Rectangle> rectangles = event.getChangedPositionRectangles();
					for (Rectangle rectangle : rectangles) {
						Range changedRange = new Range(rectangle.y, rectangle.y + rectangle.height);
						if (selectedColumnModified(changedRange)) {
							updateSelection();
							break;
						}
					}
				}
				else {
					for (StructuralDiff diff : event.getColumnDiffs()) {
						// DiffTypeEnum.CHANGE is used for resizing and
						// shouldn't result in clearing the selection
						if (diff.getDiffType() != DiffTypeEnum.CHANGE) {
							if (selectedColumnModified(diff.getBeforePositionRange())) {
								updateSelection();
								break;
							}
						}
					}
				}
			}

			if (event.isVerticalStructureChanged()) {
				// if there are no row diffs, it seems to be a complete refresh
				if (event.getRowDiffs() == null) {
					Collection<Rectangle> rectangles = event.getChangedPositionRectangles();
					for (Rectangle rectangle : rectangles) {
						Range changedRange = new Range(rectangle.y, rectangle.y + rectangle.height);
						if (selectedRowModified(changedRange)) {
							updateSelection();
							break;
						}
					}
				} else {
					// there are row diffs so we try to determine the diffs to
					// process
					for (StructuralDiff diff : event.getRowDiffs()) {
						// DiffTypeEnum.CHANGE is used for resizing and
						// shouldn't result in clearing the selection
						if (diff.getDiffType() != DiffTypeEnum.CHANGE) {
							if (selectedRowModified(diff.getBeforePositionRange())) {
								updateSelection();
								break;
							}
						}
					}
				}
			}
		}
		else {
			// keep the selection as is in case of changes
			// Note:
			// this is the the same code I posted in various forums as a
			// workaround for the cleaning of the selection on changes
			// search for PreserveSelectionStructuralChangeEventHandler to get
			// more information on this
			PositionCoordinate[] coords = this.selectionLayer.getSelectedCellPositions();
			for (PositionCoordinate coord : coords) {
				if (coord.getColumnPosition() >= this.selectionLayer.getColumnCount()
						|| coord.getRowPosition() >= this.selectionLayer.getRowCount()) {
					// if the coordinates of the selected cells are outside the
					// valid range remove the selection
					this.selectionLayer.clearSelection(
							coord.getColumnPosition(),
							coord.getRowPosition());
				}
			}
		}
	}

	@Override
	public void setClearSelectionOnChange(boolean clearSelectionOnChange) {
		super.setClearSelectionOnChange(clearSelectionOnChange);
		this.clearSelectionOnChange = clearSelectionOnChange;
	}

	public void setSelectionProvider(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;

		this.reapplySelectionJob = new Job("Apply Selection") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				reapplySelection();
				return Status.OK_STATUS;
			}
		};
	}

	private void reapplySelection() {
		int selectedColumn = getSelectedColumnPositions().length > 0 ? getSelectedColumnPositions()[0] : 0;
		selectionLayer.clear(false);
		Display.getDefault().syncExec(() -> {
			if (selectionProvider instanceof NatTableSelectionProvider) {
				// Depending on the type of Transformer for NatTableSelectionProvider, it can perform a faster selection update than setSelection().
				((NatTableSelectionProvider<?>) selectionProvider).inputChanged(selectedColumn);
			} else {
				selectionProvider.setSelection(selectionProvider.getSelection());
			}
		});
	}

	private void updateSelection() {
		if (selectionProvider == null) selectionLayer.clear();
		else JobUtils.rescheduleJob(reapplySelectionJob, 500);
	}

    private boolean selectedRowModified(Range changedRange) {
        Set<Range> selectedRows = this.selectionLayer.getSelectedRowPositions();
		//if (getSelectedRowCount() >= selectedRows.size()) {
		//	return true;
		//}
        for (Range rowRange : selectedRows) {
            if (rowRange.overlap(changedRange)) {
                return true;
            }
        }

        // if the selection layer is empty, we should clear the selection also
        if (this.selectionLayer.getRowCount() == 0 && !this.isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean selectedColumnModified(Range changedRange) {
        for (int i = changedRange.start; i <= changedRange.end; i++) {
            if (isColumnPositionSelected(i)) {
                return true;
            }
        }

        return false;
    }

}
