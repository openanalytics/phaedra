package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.MenuItemProviders;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.nattable.command.TreeColumnExpandCollapseCommand;

public class ExtendedGroupByHeaderMenuConfiguration extends GroupByHeaderMenuConfiguration {

	private final GroupByHeaderLayer groupByHeaderLayer;

	public ExtendedGroupByHeaderMenuConfiguration(NatTable natTable, GroupByHeaderLayer groupByHeaderLayer) {
		super(natTable, groupByHeaderLayer);
		this.groupByHeaderLayer = groupByHeaderLayer;
	}

	@Override
	protected PopupMenuBuilder createGroupByHeaderMenu(NatTable natTable) {
		return super.createGroupByHeaderMenu(natTable).withMenuItemProvider(
				new IMenuItemProvider() {
					@Override
					public void addMenuItem(final NatTable natTable, Menu popupMenu) {
						MenuItem menuItem = new MenuItem(popupMenu, SWT.PUSH);
						menuItem.setText("Collapse/Expand");
						menuItem.setEnabled(true);

						menuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								NatEventData natEventData = MenuItemProviders.getNatEventData(e);
								MouseEvent originalEvent = natEventData.getOriginalEvent();

								int groupByColumnIndex = ExtendedGroupByHeaderMenuConfiguration.this.groupByHeaderLayer.getGroupByColumnIndexAtXY(
										originalEvent.x, originalEvent.y);

								int groupByDepth = getGroupByDepth(groupByColumnIndex);
								natTable.doCommand(new TreeColumnExpandCollapseCommand(groupByDepth));
							};
						});
					}
				}
		);
	}

	private int getGroupByDepth(int columnIndex) {
		List<Integer> groupByColumnIndexes = groupByHeaderLayer.getGroupByModel().getGroupByColumnIndexes();
		for (int i = 0; i < groupByColumnIndexes.size(); i++) {
			if (groupByColumnIndexes.get(i) == columnIndex) {
				return i;
			}
		}
		return -1;
	}

}