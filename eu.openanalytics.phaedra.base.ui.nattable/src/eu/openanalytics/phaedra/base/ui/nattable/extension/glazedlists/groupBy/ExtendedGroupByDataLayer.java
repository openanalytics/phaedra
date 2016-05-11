package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy;

import org.eclipse.nebula.widgets.nattable.command.ILayerCommand;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByTreeFormat;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TreeList.ExpansionModel;

public class ExtendedGroupByDataLayer<T> extends GroupByDataLayer<T> {

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor) {
		super(groupByModel, eventList, columnAccessor);
	}

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor,
			boolean useDefaultConfiguration) {

		this(groupByModel, eventList, columnAccessor, null, useDefaultConfiguration);
	}

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor,
			IConfigRegistry configRegistry) {

		this(groupByModel, eventList, columnAccessor, configRegistry, true);
	}

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor,
			IConfigRegistry configRegistry, boolean useDefaultConfiguration) {

		this(groupByModel, eventList, columnAccessor, configRegistry, true, useDefaultConfiguration);
	}

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor,
			IConfigRegistry configRegistry, boolean smoothUpdates, boolean useDefaultConfiguration) {

		this(groupByModel, eventList, columnAccessor, null, configRegistry, smoothUpdates, useDefaultConfiguration);
	}

	public ExtendedGroupByDataLayer(GroupByModel groupByModel, EventList<T> eventList, IColumnPropertyAccessor<T> columnAccessor,
			ExpansionModel<Object> expansionModel, IConfigRegistry configRegistry, boolean smoothUpdates, boolean useDefaultConfiguration) {

		super(groupByModel, eventList, columnAccessor, expansionModel, configRegistry, smoothUpdates, useDefaultConfiguration);
	}

	@Override
	protected GroupByTreeFormat<T> createGroupByTreeFormat(GroupByModel groupByModel,
			IColumnAccessor<T> groupByColumnAccessor) {
		return new ExtendedGroupByTreeFormat<T>(groupByModel, groupByColumnAccessor);
	}

	@Override
	public boolean doCommand(ILayerCommand command) {
		if (command instanceof VisualRefreshCommand) {
			// handleLayerEvent is never called, so clearCache is never triggered. Deal with it here.
			clearCache();
		}
		return super.doCommand(command);
	}

}
