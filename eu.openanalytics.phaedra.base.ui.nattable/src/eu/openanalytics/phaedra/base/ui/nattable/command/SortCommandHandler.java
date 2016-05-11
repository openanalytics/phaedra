package eu.openanalytics.phaedra.base.ui.nattable.command;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.command.SortColumnCommand;
import org.eclipse.nebula.widgets.nattable.sort.event.SortColumnEvent;

import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.CustomGlazedListsSortModel;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IAsyncColumnAccessor;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

/**
 * Handle sort commands
 */
public class SortCommandHandler<T> extends AbstractLayerCommandHandler<SortColumnCommand> {

	private final CustomGlazedListsSortModel<T> sortModel;
	private final SortHeaderLayer<T> sortHeaderLayer;

	public SortCommandHandler(CustomGlazedListsSortModel<T> sortModel, SortHeaderLayer<T> sortHeaderLayer) {
		this.sortModel = sortModel;
		this.sortHeaderLayer = sortHeaderLayer;
	}

	@Override
	public boolean doCommand(final SortColumnCommand command) {
		JobUtils.runUserJob(monitor -> {
			final int columnIndex = command.getLayer().getColumnIndexByPosition(command.getColumnPosition());
			final SortDirectionEnum newSortDirection = sortModel.getSortDirection(columnIndex).getNextSortDirection();

			IColumnAccessor<T> columnAccessor = sortModel.getColumnAccessor();
			try {
				// Make active during sorting so not loaded values are correctly sorted.
				if (columnAccessor instanceof IAsyncColumnAccessor) {
					((IAsyncColumnAccessor<T>) columnAccessor).setAsync(false);
				}

				sortModel.sort(columnIndex, newSortDirection, command.isAccumulate());
			} finally {
				if (columnAccessor instanceof IAsyncColumnAccessor) {
					((IAsyncColumnAccessor<T>) columnAccessor).setAsync(true);
				}
			}

			// Fire event
			SortColumnEvent sortEvent = new SortColumnEvent(sortHeaderLayer, command.getColumnPosition());
			sortHeaderLayer.fireLayerEvent(sortEvent);
		}, "Sorting column...", IProgressMonitor.UNKNOWN, null, null);

		// Fire command - with busy indicator
		//Runnable sortRunner = new Runnable() {
		//	@Override
		//	public void run() {
		//		sortModel.sort(columnIndex, newSortDirection, command.isAccumulate());
		//	}
		//};
		//BusyIndicator.showWhile(null, sortRunner);

		// Fire event
		//SortColumnEvent sortEvent = new SortColumnEvent(sortHeaderLayer, command.getColumnPosition());
		//sortHeaderLayer.fireLayerEvent(sortEvent);

		return true;
	}

	@Override
	public Class<SortColumnCommand> getCommandClass() {
		return SortColumnCommand.class;
	}

}
