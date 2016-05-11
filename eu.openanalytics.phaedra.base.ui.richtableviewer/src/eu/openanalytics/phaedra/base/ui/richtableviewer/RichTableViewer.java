package eu.openanalytics.phaedra.base.ui.richtableviewer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.swt.IFocusService;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomizeColumnsDialog;
import eu.openanalytics.phaedra.base.ui.richtableviewer.state.IStateStore;
import eu.openanalytics.phaedra.base.ui.richtableviewer.state.StateStoreRegistry;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ImageLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.RichTableFilter;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;
import eu.openanalytics.phaedra.base.ui.util.misc.SearchBar;
import eu.openanalytics.phaedra.base.ui.util.misc.SearchBar.ISearchHandler;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class RichTableViewer extends TableViewer {

	public final static int NO_SEPARATOR = 1 << 10;

	private String tableKey;
	private String storeId;

	private SearchBar searchBar;
	private RichTableFilter searchFilter;

	private Set<ColumnDataType> dataTypes;

	private int selConf;

	public RichTableViewer(Table table) {
		this(table, null);
	}

	public RichTableViewer(Table table, String tableKey) {
		super(table);
		initTable(tableKey, false);
		ContextHelper.attachContext(getTable(), CopyItems.COPY_PASTE_CONTEXT_ID);
	}

	public RichTableViewer(Composite parent, int style) {
		this(parent, style, null);
	}

	public RichTableViewer(Composite parent, int style, String tableKey) {
		this(parent, style, tableKey, false);
	}

	public RichTableViewer(Composite parent, int style, String tableKey, boolean searchEnabled) {
		super(createTableContainer(parent, searchEnabled, style), style | SWT.FULL_SELECTION | SWT.MULTI);
		if (searchEnabled) {
			getTable().addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					// Dispose the searchbar too.
					getTable().getParent().getParent().dispose();
				}
			});
		}
		initTable(tableKey, searchEnabled);

		// Track focus of table, this is required for command handlers that want to know which table triggered the command.
		IFocusService service = (IFocusService)PlatformUI.getWorkbench().getService(IFocusService.class);
		service.addFocusTracker(getTable(), "richTableViewer");
		getTable().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				IFocusService service = (IFocusService)PlatformUI.getWorkbench().getService(IFocusService.class);
				service.removeFocusTracker(getTable());
			}
		});

		ContextHelper.attachContext(getTable(), CopyItems.COPY_PASTE_CONTEXT_ID);
	}

	@Override
	public ISelection getSelection() {
		Control control = getControl();
		if (control == null || control.isDisposed()) {
			return StructuredSelection.EMPTY;
		}
		List<?> list = getSelectionFromWidget();
		ConfigurableStructuredSelection sel = new ConfigurableStructuredSelection(list, getComparer());
		sel.setConfiguration(selConf);
		return sel;
	}
	
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		super.inputChanged(input, oldInput);
		if (getTable().getSortColumn() != null) {
			//TODO Force sort
		}
	}

	public void setStoreId(String storeId) {
		// Must be called before columns are configured.
		this.storeId = storeId;
	}

	public void contributeConfigButton(IMenuManager manager) {
		manager.add(new Separator());
		Action configureColumnsAction = new Action("Configure Columns...") {
			@Override
			public void run() {
				CustomizeColumnsDialog dialog = createConfigureColumnDialog();
				dialog.open();
			}
		};
		configureColumnsAction.setImageDescriptor(IconManager.getIconDescriptor("table_column.gif"));
		manager.add(configureColumnsAction);
	}

	public void applyColumnConfig(List<ColumnConfiguration> configs) {
		applyColumnConfig(configs.toArray(new ColumnConfiguration[configs.size()]));
	}
	
	public void applyColumnConfig(ColumnConfiguration[] configs) {
		getTable().setRedraw(false);
		// If there currently are columns present, dispose them first.
		TableColumn[] existingColumns = getTable().getColumns();
		boolean hadColumns = existingColumns.length > 0;
		if (hadColumns) {
			// Prevent error when custom LabelProvider was set.
			setLabelProvider(new ColumnLabelProvider());
			for (TableColumn col : existingColumns) {
				col.dispose();
			}
		}
		dataTypes.clear();

		ColumnConfiguration[] savedConfigs = null;
		IStateStore store = StateStoreRegistry.getStore(storeId);
		if (tableKey != null && store != null) {
			try {
				savedConfigs = store.loadState(tableKey);
			} catch (IOException e) {
				EclipseLog.warn("Failed to load column state", e, Activator.getDefault());
			}
		}

		ColumnConfiguration[] mergedConfigs = merge(configs, savedConfigs);
		
		for (ColumnConfiguration config : mergedConfigs) {

			TableViewerColumn col = new TableViewerColumn(this, SWT.NONE);
			col.getColumn().setText(config.getName());
			col.getColumn().setWidth(config.getWidth());
			col.getColumn().setToolTipText(config.getTooltip());
			col.getColumn().setMoveable(config.isMovable());

			CellLabelProvider labelProvider = config.getLabelProvider();
			if (labelProvider == null)
				labelProvider = new RichLabelProvider(config);
			if (labelProvider instanceof ImageLabelProvider)
				col.getColumn().setData("Image", true);
			col.setLabelProvider(labelProvider);

			Comparator<?> sorter = config.getSorter();
			if (sorter != null) {
				ColumnViewerSorter<?> colSorter = new ColumnViewerSorter<>(this, col, sorter);
				if (config.getSortDirection() != SWT.NONE) {
					colSorter.setSorter(config.getSortDirection());
				}
			}

			ColumnEditingFactory.apply(col, config.getEditingConfig());
			
			if (config.isHidden()) {
				col.getColumn().setWidth(0);
				col.getColumn().setResizable(false);
			}
			
			col.getColumn().setData(config);
			dataTypes.add(config.getDataType());
		}

		if (searchBar != null) {
			List<String> names = new ArrayList<String>();
			for (ColumnConfiguration config: configs) {
				String name = config.getName();
				if (config.getLabelProvider() instanceof RichLabelProvider) {
					if (name != null && !name.isEmpty()) names.add(name);
				}
			}
			searchBar.setNames(names.toArray(new String[names.size()]));
			searchFilter.setColumns(configs);
		}

		getTable().setRedraw(true);
		if (hadColumns) refresh();
	}

	public void resizeTable() {
		getTable().setRedraw(false);
		boolean resizeRows = false;
		for (TableColumn tc : getTable().getColumns()) {
			if (tc.getWidth() != 0) tc.pack();
			else if (tc.getData("Image") != null && (boolean) tc.getData("Image")) resizeRows = true;
		}
		// If the Image Column is hidden, set size on 18.
		if (resizeRows) setTableItemHeight(getTable(), 18);
		getTable().setRedraw(true);
	}

	/**
	 * Fix to manually set item height.
	 * @param Table to resize
	 * @param The new item height
	 */
	public static void setTableItemHeight(final Table table, final int itemHeight) {
		try {
			// Search class hierarchy for protected setter method
			final Method setter = Table.class.getDeclaredMethod("setItemHeight", int.class);
			boolean accessible = setter.isAccessible();
			setter.setAccessible(true);
			setter.invoke(table, itemHeight);
			setter.setAccessible(accessible);
		} catch (Exception e) {
			// Stick with the old height.
		}
	}

	public ColumnConfiguration[] getCurrentColumnState() {
		TableColumn[] columns = getTable().getColumns();
		int[] order = getTable().getColumnOrder();
		ColumnConfiguration[] configs = new ColumnConfiguration[columns.length];
		
		for (int i = 0; i < columns.length; i++) {
			int orderedIndex = order[i];
			TableColumn column = columns[orderedIndex];
			configs[i] = (ColumnConfiguration) column.getData();
			if (column.getWidth() != 0) {
				configs[i].setWidth(column.getWidth());
			}
			configs[i].setHidden(column.getWidth() == 0);
			configs[i].setSortDirection((column == getTable().getSortColumn()) ? getTable().getSortDirection() : 0);
		}

		return configs;
	}

	public void saveColumnState() {
		IStateStore store = StateStoreRegistry.getStore(storeId);
		if (tableKey == null || store == null) return;

		try {
			ColumnConfiguration[] configs = getCurrentColumnState();
			store.saveState(configs, tableKey);
		} catch (IOException e) {
			EclipseLog.warn("Failed to save column state", e, Activator.getDefault());
		}
	}

	public void setDefaultSearchColumn(String name) {
		if (searchBar != null) searchBar.setCurrentName(name);
	}

	public boolean isSearchable() {
		return searchFilter != null;
	}

	public boolean hasColumnDataType(ColumnDataType type) {
		return dataTypes.contains(type);
	}

	public int getSelectionConfiguration() {
		return selConf;
	}

	public void setSelectionConfiguration(int selConf) {
		this.selConf = selConf;
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	protected CustomizeColumnsDialog createConfigureColumnDialog() {
		return new CustomizeColumnsDialog(Display.getDefault().getActiveShell(), RichTableViewer.this);
	}

	private void initTable(String tableKey, boolean searchEnabled) {
		if (searchEnabled) {
			searchFilter = new RichTableFilter();
			addFilter(searchFilter);

			Composite toolBar = (Composite)getTable().getParent().getParent().getChildren()[0];

			Composite searchArea = new Composite(toolBar, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,false).applyTo(searchArea);
			GridLayoutFactory.fillDefaults().margins(3,3).spacing(0,0).applyTo(searchArea);
			addSearchbar(searchArea);

			Composite toolBarButtons = new Composite(toolBar, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true,false).applyTo(toolBarButtons);
			GridLayoutFactory.fillDefaults().margins(3,3).spacing(0,0).applyTo(toolBarButtons);
			Button resizeBtn = new Button(toolBarButtons, SWT.PUSH);
			resizeBtn.setToolTipText("Resize table columns");
			resizeBtn.setImage(IconManager.getIconImage("table.png"));
			resizeBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					resizeTable();
				}
			});
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(resizeBtn);
		}

		Table table = getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		// Careful: listen to parent's disposal, because saveColumnState needs access to Table,
		// so Table itself must not be disposed yet.
		table.getParent().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				saveColumnState();
			}
		});

		this.tableKey = tableKey;
		this.storeId = StateStoreRegistry.DEFAULT_STORE_ID;
		this.dataTypes = new HashSet<>();

		if (searchEnabled) {
			GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		}
	}

	private void addSearchbar(Composite parent) {
		searchBar = new SearchBar(parent, SWT.NONE, true);
		GridDataFactory.fillDefaults().applyTo(searchBar);

		searchBar.setSearchHandler(new ISearchHandler() {
			@Override
			public void doSearch(final String name, final String value) {
				searchFilter.setColumnName(name);
				searchFilter.setSearchText(value);
				getControl().setRedraw(false);
				refresh(true);
				getControl().setRedraw(true);
			}

			@Override
			public void doClear() {
				searchFilter.setSearchText(null);
			}
		});
	}

	private static Composite createTableContainer(Composite parent, boolean searchEnabled, int style) {
		if (!searchEnabled) return parent;

		Composite c = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0,0).applyTo(c);

		Composite toolBar = new Composite(c, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).applyTo(toolBar);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(0,0).spacing(0,0).applyTo(toolBar);

		if (NO_SEPARATOR != (style & NO_SEPARATOR)) {
			Label lbl = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);
		}

		Composite tableArea = new Composite(c, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableArea);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		return tableArea;
	}

	private ColumnConfiguration[] merge(ColumnConfiguration[] runtimeConfigs, ColumnConfiguration[] savedConfigs) {
		if (savedConfigs == null || savedConfigs.length == 0) return runtimeConfigs;
		
		List<ColumnConfiguration> merged = new ArrayList<>();
		List<ColumnConfiguration> remaining = Lists.newArrayList(runtimeConfigs);
		
		// Merge runtime and saved configs in the order of the saved configs.
		Arrays.stream(savedConfigs).forEach(sc -> {
			ColumnConfiguration rt = Arrays.stream(runtimeConfigs).filter(c -> c.getKey().equals(sc.getKey())).findFirst().orElse(null);
			if (rt != null) {
				rt.setWidth(sc.getWidth());
				rt.setSortDirection(sc.getSortDirection());
				if (sc.isHidden()) rt.setWidth(0);
				merged.add(rt);
				remaining.remove(rt);
			}
		});
		
		merged.addAll(remaining);
		return merged.toArray(new ColumnConfiguration[merged.size()]);
	}
}
