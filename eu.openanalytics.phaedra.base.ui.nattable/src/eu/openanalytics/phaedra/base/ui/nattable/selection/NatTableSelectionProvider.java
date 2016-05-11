package eu.openanalytics.phaedra.base.ui.nattable.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionCoordinate;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionUtil;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByObject;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.selection.ISelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowObjectIndexHolder;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.command.SelectCellCommand;
import org.eclipse.nebula.widgets.nattable.selection.event.ISelectionEvent;
import org.eclipse.nebula.widgets.nattable.selection.event.RowSelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;

public class NatTableSelectionProvider<T> extends EventManager implements ISelectionProvider, ILayerListener {

	private final SelectionLayer selectionLayer;
	private final IRowDataProvider<T> rowDataProvider;

	private final boolean fullySelectedRowsOnly;
	private final boolean handleSameRowSelection;

	private boolean addSelectionOnSet;
	private int selConfig;

	private ConfigurableStructuredSelection currentSelection;
	private List<T> currentListSelection;

	private ISelectionTransformer<T> transformer;

	private Job fireSelectionJob;

	public NatTableSelectionProvider(FullFeaturedColumnHeaderLayerStack<T> columnHeaderLayer, IRowDataProvider<T> rowDataProvider
			, boolean fullySelectedRowsOnly, boolean handleSameRowSelection, ISelectionTransformer<T> transformer) {

		this.selectionLayer = columnHeaderLayer.getSelectionLayer();
		this.rowDataProvider = rowDataProvider;

		this.fullySelectedRowsOnly = fullySelectedRowsOnly;
		this.handleSameRowSelection = handleSameRowSelection;

		this.addSelectionOnSet = false;

		this.transformer = transformer;

		this.currentSelection = new ConfigurableStructuredSelection();
		this.currentListSelection = new ArrayList<>();

		this.fireSelectionJob = new Job("Fire Selection") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().syncExec(() -> fireSelection());
				return Status.OK_STATUS;
			}
		};

		selectionLayer.addLayerListener(this);
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		addListenerObject(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		removeListenerObject(listener);
	}

	@Override
	public ISelection getSelection() {
		return currentSelection;
	}

	public List<T> getCurrentListselection() {
		return currentListSelection;
	}

	@Override
	public void setSelection(ISelection selection) {
		if (selectionLayer != null && selection instanceof IStructuredSelection) {
			if (!selection.isEmpty()) {
				List<T> newSelection = transformer.transformIngoingSelection(selection);
				if (newSelection == null || newSelection.equals(currentListSelection)) return;

				selectionLayer.clear(false);
				currentListSelection = newSelection;
				populateLayer(0);
			}
		}
	}

	@Override
	public void handleLayerEvent(ILayerEvent event) {
		if (event instanceof ISelectionEvent) {
			fireUpdatedSelection();
		}
	}

	/**
	 * This method should be called when the input of the NatTable changed.
	 */
	public void inputChanged(int columnToSelect) {
		if (transformer instanceof SelectionTransformer) {
			// The default transformer that returns the given objects (e.g. SubWellItem to SubWellItem).
			populateLayer(columnToSelect);
		} else {
			// A customized transformer that transforms objects to different Objects (e.g. Integer to SubWellItem).
			// By just using populateLayer(columnToSelect), one would reselect the previous selected indexes even
			// if they no longer represent the same object.
			setSelection(getSelection());
		}
	}

	/**
	 * Get the current selected items from the table and fire them.
	 */
	public void fireUpdatedSelection() {
		ConfigurableStructuredSelection selection = populateRowSelection();
		if (handleSameRowSelection || !selection.equals(currentSelection)) {
			currentSelection = selection;

			// Because an event is send for every Cell selection (even while dragging) schedule the fireSelection().
			fireSelectionJob.cancel();
			fireSelectionJob.schedule(200);
		}
	}

	public IRowDataProvider<T> getRowDataProvider() {
		return rowDataProvider;
	}

	public void setSelectionConfiguration(int selConfig) {
		this.selConfig = selConfig;
	}

	private void fireSelection() {
		SelectionChangedEvent event = new SelectionChangedEvent(this, currentSelection);
		for (Object listener: getListeners()) {
			((ISelectionChangedListener)listener).selectionChanged(event);
		}
	}

	private void populateLayer(int columnToSelect) {
		Set<Integer> rowPositions = Collections.synchronizedSet(new HashSet<>());

		Set<T> set = new HashSet<>(currentListSelection);
		IntStream.range(0, rowDataProvider.getRowCount()).parallel().forEach(i -> {
			try {
				T rowObject = rowDataProvider.getRowObject(i);
				if (set.contains(rowObject)) rowPositions.add(i);
			} catch (IndexOutOfBoundsException e) {
				// Sync issue.
				return;
			}
		});

		int intValue = -1;
		if (!rowPositions.isEmpty()) {
			Integer max = Collections.max(rowPositions);
			intValue = max.intValue();
		}
		if (intValue >= 0) {
			if (rowPositions.size() == selectionLayer.getRowCount()) {
				// All layers are selected, use the SelectAllCommand for performance reasons.
				//selectionLayer.clear(false);
				selectionLayer.addSelection(new Rectangle(0, 0, selectionLayer.getColumnCount(), selectionLayer.getRowCount()));
				selectionLayer.fireLayerEvent(new RowSelectionEvent(selectionLayer, rowPositions, -1));
				/*
				 * The logical way to select the whole table would be by using
				 * selectionLayer.doCommand(new SelectAllCommand());
				 *
				 *  The downside of using this command is that the table will move the viewport to the top left corner of the table.
				 *  This behavior is not wanted when e.g. sorting.
				 */
			} else if (rowPositions.size() == 1 && columnToSelect != 0) {
				selectionLayer.doCommand(
						new SelectCellCommand(selectionLayer, columnToSelect, rowPositions.iterator().next(), false, addSelectionOnSet));
			} else {
				// The standard way for selecting rows is much slower on large selections (144 000ms vs 350ms on 80k rows).
				//selectionLayer.doCommand(
				//		new SelectRowsCommand(selectionLayer, columnToSelect, ObjectUtils.asIntArray(rowPositions), false, addSelectionOnSet, intValue));
				List<Range> ranges = PositionUtil.getRanges(rowPositions);

				ISelectionModel selectionModel = selectionLayer.getSelectionModel();
				if (selectionModel instanceof CachedSelectionModel) {
					CachedSelectionModel model = (CachedSelectionModel) selectionModel;
					boolean performCheck = !model.isEmpty();
					for (Range r : ranges) {
						Rectangle range = new Rectangle(0, r.start, Integer.MAX_VALUE, r.size());
						model.addSelection(range, performCheck);
					}
				} else {
					for (Range r : ranges) {
						Rectangle range = new Rectangle(0, r.start, Integer.MAX_VALUE, r.size());
						selectionLayer.addSelection(range);
					}
				}
				selectionLayer.fireLayerEvent(new RowSelectionEvent(selectionLayer, rowPositions, intValue));
			}
		} else {
			/*
			 * When we get here, it can mean one of the following:
			 * - The selected data is no longer present in the current input;
			 * - The selected data is hidden by a filter.
			 *
			 * Clear selection on selection layer since selection is no longer visible, this should be done in both cases.
			 *
			 * Clear currentSelection which will prevent sending out the wrong selection (previous) if new input is used.
			 * CurrentListselection is kept so it can restore the previous selection if it was just hidden by e.g. a filter.
			 */
			selectionLayer.clear();
			selectionLayer.doCommand(new VisualRefreshCommand());

			this.currentSelection = new ConfigurableStructuredSelection();
			//this.currentListSelection = new ArrayList<>();
		}
	}

	private ConfigurableStructuredSelection populateRowSelection() {
		List<RowObjectIndexHolder<T>> rows = new ArrayList<RowObjectIndexHolder<T>>();

		if (selectionLayer != null) {
			if (fullySelectedRowsOnly) {
				for (int rowPosition : selectionLayer.getFullySelectedRowPositions()) {
					addToSelection(rows, rowPosition);
				}
			} else {
				Set<Range> rowRanges = selectionLayer.getSelectedRowPositions();
				for (Range rowRange : rowRanges) {
					for (int rowPosition = rowRange.start; rowPosition < rowRange.end; rowPosition++) {
						addToSelection(rows, rowPosition);
					}
				}
			}
		}
		Collections.sort(rows);
		currentListSelection.clear();
		for(RowObjectIndexHolder<T> holder : rows){
			currentListSelection.add(holder.getRow());
		}
		ConfigurableStructuredSelection sel;
		if (rows.isEmpty()) {
			sel = new ConfigurableStructuredSelection();
		} else if (transformer instanceof ColumnSelectionTransformer) {
			ColumnSelectionTransformer<T> columnTransformer = (ColumnSelectionTransformer<T>) transformer;
			PositionCoordinate[] selectedCellPositions = selectionLayer.getSelectedCellPositions();
			List<?> outgoingSelection = columnTransformer.transformOutgoingSelection(currentListSelection
					, rowDataProvider, selectedCellPositions);
			sel = new ConfigurableStructuredSelection(outgoingSelection);
		} else {
			List<?> outgoingSelection = transformer.transformOutgoingSelection(currentListSelection);
			sel = new ConfigurableStructuredSelection(outgoingSelection);
		}
		sel.setConfiguration(selConfig);
		return sel;
	}

	private void addToSelection(List<RowObjectIndexHolder<T>> rows, int rowPosition) {
		int rowIndex = selectionLayer.getRowIndexByPosition(rowPosition);
		if (rowIndex >= 0 && rowIndex < rowDataProvider.getRowCount()) {
			T rowObject = rowDataProvider.getRowObject(rowIndex);
			// Do not select GroupByObjects since they are not transformable.
			if (!(rowObject instanceof GroupByObject)) {
				rows.add(new RowObjectIndexHolder<T>(rowIndex, rowObject));
			}
		}
	}

	/**
	 * Configure whether <code>setSelection()</code> should add or set the selection.
	 * <p>
	 * This was added for convenience because the initial code always added the selection
	 * on <code>setSelection()</code> by creating a SelectRowsCommand with the withControlMask set to <code>true</code>.
	 * Looking at the specification, <code>setSelection()</code> is used to set the <b>new</b> selection.
	 * So the default here is now to set instead of add. But for convenience to older code
	 * that relied on the add behaviour it is now possible to change it back to adding.
	 *
	 * @param addSelectionOnSet <code>true</code> to add the selection on calling <code>setSelection()</code>
	 * 			The default is <code>false</code> to behave like specified in RowSelectionProvider
	 */
	public void setAddSelectionOnSet(boolean addSelectionOnSet) {
		this.addSelectionOnSet = addSelectionOnSet;
	}

}