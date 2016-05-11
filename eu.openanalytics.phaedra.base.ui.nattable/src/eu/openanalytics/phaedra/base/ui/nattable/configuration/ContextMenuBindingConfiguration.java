package eu.openanalytics.phaedra.base.ui.nattable.configuration;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.edit.command.EditSelectionCommand;
import org.eclipse.nebula.widgets.nattable.export.command.ExportCommand;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.summary.IGroupBySummaryProvider;
import org.eclipse.nebula.widgets.nattable.freeze.action.FreezeGridAction;
import org.eclipse.nebula.widgets.nattable.freeze.action.UnFreezeGridAction;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.MenuItemProviders;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.INatTableMenuContributor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.PrePopupSelectAction;

public class ContextMenuBindingConfiguration extends AbstractUiBindingConfiguration {

	private final Menu colHeaderMenu;
	private final Menu rowHeaderMenu;
	private final Menu cornerMenu;
	private Menu bodyMenu;

	/**
	 * Add a context menu to the NatTable.
	 * @param natTable The Table.
	 * @param menuMgr Optional, this menu will be used for the Body of the Table.
	 */
	public ContextMenuBindingConfiguration(final NatTable natTable, MenuManager menuMgr,
			final boolean editable) {

		// Default column context menu
		colHeaderMenu = new PopupMenuBuilder(natTable)
			.withHideColumnMenuItem()
			.withShowAllColumnsMenuItem()
			.withColumnChooserMenuItem()
			.withSeparator()
			.withCreateColumnGroupsMenuItem()
			.withUngroupColumnsMenuItem()
			.withSeparator()
			.withMenuItemProvider(getExportMenuItem())
			.withAutoResizeSelectedColumnsMenuItem()
			.withColumnRenameDialog()
			.withClearAllFilters()
			.withMenuItemProvider(getColumnFormatterMenuItem())
			.build();

		rowHeaderMenu = new PopupMenuBuilder(natTable)
		// With a big selection, auto resizing hangs up Phaedra.
		// Manually drag 1 row to change heights of everything to a specific value.
		//.withAutoResizeSelectedRowsMenuItem()
			.build();

		IMenuListener listener;
		if (menuMgr != null) {
			listener = (manager) -> {
				manager.add(new ContributionItem() {
					@Override
					public void fill(Menu menu, int index) {
						// If natTable is rebuild from the menu, it will attempt to refresh once with natTable == null.
						if (natTable.isDisposed()) return;
						bodyMenu = new PopupMenuBuilder(natTable, menu).build();
						fillBodyMenu(natTable, editable);
					}
					@Override
					public boolean isDynamic() {
						return true;
					}
				});
			};
			menuMgr.addMenuListener(listener);
		} else {
			listener = null;
			bodyMenu = new PopupMenuBuilder(natTable).build();
			fillBodyMenu(natTable, editable);
		}

		cornerMenu = new PopupMenuBuilder(natTable)
			.withHideColumnMenuItem()
			.withShowAllColumnsMenuItem()
			.withColumnChooserMenuItem()
			.build();

		natTable.addListener(SWT.Dispose, e -> {
			colHeaderMenu.dispose();
			rowHeaderMenu.dispose();
			cornerMenu.dispose();
			if (bodyMenu != null) bodyMenu.dispose();
			if (listener != null) menuMgr.removeMenuListener(listener);
		});
	}

	@Override
	public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.COLUMN_HEADER, MouseEventMatcher.RIGHT_BUTTON),
				new PopupMenuAction(this.colHeaderMenu));
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.ROW_HEADER, MouseEventMatcher.RIGHT_BUTTON),
				new PopupMenuAction(this.rowHeaderMenu));
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
				new PrePopupSelectAction());
		// If bodyMenu is null it means we are using a MenuManager for the body context menu.
		if (bodyMenu != null) {
			uiBindingRegistry.registerMouseDownBinding(
					new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
					new PopupMenuAction(this.bodyMenu));
		}
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.CORNER, MouseEventMatcher.RIGHT_BUTTON),
				new PopupMenuAction(this.cornerMenu));
	};

	protected void fillBodyMenu(final NatTable natTable, final boolean editable) {
		// Prevent MenuItems from showing up twice when a NatTable component is added in multiple tabs for a CTabFolder.
		if (!natTable.isDisposed() && natTable.isVisible()) {
			if (bodyMenu.getItemCount() > 0) {
				new MenuItem(bodyMenu, SWT.SEPARATOR);
			}

			MenuItem menuItem;
			if (editable) {
				menuItem = new MenuItem(bodyMenu, SWT.PUSH);
				menuItem.setText("Edit Selected Cell(s)");
				menuItem.setImage(IconManager.getIconImage("table_edit.png"));
				menuItem.setEnabled(true);
				menuItem.addListener(SWT.Selection, event -> {
					natTable.doCommand(new EditSelectionCommand(natTable, natTable.getConfigRegistry()));
				});
			}

			menuItem = new MenuItem(bodyMenu, SWT.PUSH);
			menuItem.setText("Freeze column");
			menuItem.setImage(IconManager.getIconImage("chain.png"));
			menuItem.setEnabled(true);
			menuItem.addListener(SWT.Selection, (event) -> {
				new FreezeGridAction(false, true).run(natTable, null);
			});

			menuItem = new MenuItem(bodyMenu, SWT.PUSH);
			menuItem.setText("Unfreeze column");
			menuItem.setImage(IconManager.getIconImage("chain_off.png"));
			menuItem.setEnabled(true);
			menuItem.addListener(SWT.Selection, (event) -> {
				new UnFreezeGridAction().run(natTable, null);
			});
		}
	}

	private IMenuItemProvider getColumnFormatterMenuItem() {
		return (table, popupMenu) -> {
			NatEventData natEventData = getNatEventData(popupMenu);
			NatTable natTable = natEventData.getNatTable();
			int columnPosition = natEventData.getColumnPosition();


			boolean isSeparatorAdded = false;

			String columnName = NatTableUtils.getColumnName(natTable, columnPosition);
			IDisplayConverter displayConverter = natTable.getConfigRegistry().getConfigAttribute(
					CellConfigAttributes.DISPLAY_CONVERTER,
					DisplayMode.NORMAL,
					columnName
			);
			if (displayConverter instanceof INatTableMenuContributor) {
				if (!isSeparatorAdded) {
					isSeparatorAdded = !isSeparatorAdded;
					new MenuItem(popupMenu, SWT.SEPARATOR);
				}
				((INatTableMenuContributor) displayConverter).fillMenu(natTable, popupMenu);
			}

			IGroupBySummaryProvider<?> summaryProvider = natTable.getConfigRegistry().getConfigAttribute(
				GroupByConfigAttributes.GROUP_BY_SUMMARY_PROVIDER
				, DisplayMode.NORMAL
				, columnName
			);
			if (summaryProvider instanceof INatTableMenuContributor) {
				if (!isSeparatorAdded) {
					isSeparatorAdded = !isSeparatorAdded;
					new MenuItem(popupMenu, SWT.SEPARATOR);
				}
				((INatTableMenuContributor) summaryProvider).fillMenu(natTable, popupMenu);
			}
		};
	}

	private IMenuItemProvider getExportMenuItem() {
		return (table, popupMenu) -> {
			MenuItem menuItem = new MenuItem(popupMenu, SWT.PUSH);
			menuItem.setText("Export Table");
			menuItem.setImage(IconManager.getIconImage("page_excel.png"));
			menuItem.setEnabled(true);
			menuItem.addListener(SWT.Selection, e -> table.doCommand(new ExportCommand(table.getConfigRegistry(), table.getShell())));
		};
	}

    private static NatEventData getNatEventData(Widget widget) {
    	Object data = widget.getData(MenuItemProviders.NAT_EVENT_DATA_KEY);
        return data != null ? (NatEventData) data : null;
    }

}