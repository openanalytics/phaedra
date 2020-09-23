package eu.openanalytics.phaedra.ui.plate.chart.v2.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterItemsSelectionDialog;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;

public class WellTypeFilter extends AbstractWellFilter {

	private final static List<String> ALL_TYPES = new ArrayList<String>() {
		private static final long serialVersionUID = 3032882624898284813L;
		{
			addAll(CollectionUtils.transform(ProtocolService.getInstance().getWellTypes(), ProtocolUtils.WELLTYPE_CODES));
		}
	};

	public WellTypeFilter(WellDataProvider dataProvider) {
		super("Well Type", dataProvider);
		setFilterItems(ALL_TYPES);
		setActiveFilterItems(new ArrayList<>());
	}

	@Override
	public void doInitialize(Menu parent) {
		// Filter item
		MenuItem item = new MenuItem(parent, SWT.PUSH);
		item.setText(getGroup() + (isActive() ? " [Enabled]" : ""));
		item.addListener(SWT.Selection, e -> {
			List<String> activeFilterItems = getActiveFilterItems();
			if (activeFilterItems.isEmpty()) activeFilterItems.addAll(ALL_TYPES);
			FilterItemsSelectionDialog dialog = new FilterItemsSelectionDialog("Well Type Filter", "Uncheck Well Types to hide them."
					, getFilterItems(), activeFilterItems);
			if (dialog.open() == IDialogConstants.OK_ID) {
				activeFilterItems = dialog.getActiveFilterItems();
				if (activeFilterItems.containsAll(ALL_TYPES)) activeFilterItems.clear();
				setActiveFilterItems(activeFilterItems);
				doApplyFilterItem(activeFilterItems.isEmpty() ? null : activeFilterItems.get(0));
			}
			item.setText(getGroup() + (isActive() ? " [Enabled]" : ""));
		});
	}

	@Override
	public void doApplyFilterItem(String filterItem) {
		// Support for older Saved Views.
		if ("All".equalsIgnoreCase(filterItem)) setActiveFilterItems(new ArrayList<>());

		super.doApplyFilterItem(filterItem);
	}

	@Override
	public boolean isActive() {
		// If no items or all items are selected, it shows everything anyway.
		return !getActiveFilterItems().isEmpty() && getActiveFilterItems().size() != ALL_TYPES.size();
	}

	@Override
	protected String getKey(Well well) {
		//PHA-644
		return ProtocolUtils.getCustomHCLCLabel(well.getWellType());
	}

}