package eu.openanalytics.phaedra.base.ui.richtableviewer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.CustomColumnSupport;
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
	
	private final static String VIEWER_COLUMN_KEY = "org.eclipse.jface.columnViewer";
	private final static String SORTER_KEY = "eu.openanalytics.phaedra.base.ui.richtableviewer.Sorter";
	
	protected static final ColumnConfiguration getConfig(final TableColumn column) {
		return (ColumnConfiguration)column.getData();
	}
	
	
	private String tableKey;
	private String storeId;
	private IStateStore stateStore;
	private final Consumer<IStateStore.StateChangedEvent> stateStoreListener = this::onStateChanged;
	
	private CustomColumnSupport customColumnSupport;

	private SearchBar searchBar;
	private RichTableFilter searchFilter;

	private Set<DataType> dataTypes;

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

	public RichTableViewer(Composite parent, int style, String tableKey, CustomColumnSupport customColumnSupport,
			boolean searchEnabled) {
		super(createTableContainer(parent, searchEnabled, style), style | SWT.FULL_SELECTION | SWT.MULTI);
		
		this.customColumnSupport = customColumnSupport;
		
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
		IFocusService service = PlatformUI.getWorkbench().getService(IFocusService.class);
		service.addFocusTracker(getTable(), "richTableViewer");
		getTable().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disconnectStateStore();
				
				IFocusService service = PlatformUI.getWorkbench().getService(IFocusService.class);
				service.removeFocusTracker(getTable());
			}
		});

		ContextHelper.attachContext(getTable(), CopyItems.COPY_PASTE_CONTEXT_ID);
	}
	
	public RichTableViewer(Composite parent, int style, String tableKey, boolean searchEnabled) {
		this(parent, style, tableKey, null, searchEnabled);
	}
	
	
	public CustomColumnSupport getCustomColumnSupport() {
		return this.customColumnSupport;
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
	
	private IStateStore getStateStore() {
		final String tableKey = this.tableKey;
		if (tableKey == null) {
			return null;
		}
		IStateStore stateStore = this.stateStore;
		if (stateStore == null) {
			stateStore = StateStoreRegistry.getStore(this.storeId);
			if (stateStore != null) {
				this.stateStore = stateStore;
				stateStore.addListener(tableKey, this.stateStoreListener);
			}
		}
		return stateStore;
	}
	
	private void disconnectStateStore() {
		IStateStore stateStore = this.stateStore;
		if (stateStore != null) {
			this.stateStore = null;
			stateStore.removeListener(tableKey, this.stateStoreListener);
		}
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


	private List<ColumnConfiguration> getCurrentConfigs(final boolean ordered, final boolean updateView) {
		final TableColumn[] columns = getTable().getColumns();
		final List<ColumnConfiguration> list = new ArrayList<>(columns.length);
		final int[] order = (ordered) ? getTable().getColumnOrder() : null;
		for (int i = 0; i < columns.length; i++) {
			final TableColumn column = columns[(order != null) ? order[i] : i];
			final ColumnConfiguration config = getConfig(column);
			if (updateView) {
				updateConfigFromView(config, column);
			}
			list.add(config);
		}
		return list;
	}
	
	public List<ColumnConfiguration> getColumnConfigs(final boolean ordered) {
		return getCurrentConfigs(ordered, true);
	}
	
	public void applyColumnConfig(List<ColumnConfiguration> configs) {
		applyColumnConfig(configs.toArray(new ColumnConfiguration[configs.size()]));
	}
	
	public void applyColumnConfig(ColumnConfiguration[] configs) {
		TableColumn[] existingColumns = getTable().getColumns();
		boolean hadColumns = existingColumns.length > 0;
		
		getTable().setRedraw(false);
		try {
			// If there currently are columns present, dispose them first.
			if (hadColumns) {
				// Prevent error when custom LabelProvider was set.
				setLabelProvider(new ColumnLabelProvider());
				for (TableColumn column : existingColumns) {
					column.dispose();
				}
			}
			dataTypes.clear();
	
			List<ColumnConfiguration> savedConfigs = null;
			IStateStore store = getStateStore();
			if (store != null) {
				try {
					ColumnConfiguration[] array = store.loadState(tableKey);
					savedConfigs = (array != null) ? Arrays.asList(array) : null;
				} catch (IOException e) {
					EclipseLog.warn("Failed to load column state", e, Activator.getDefault());
				}
			}
			List<ColumnConfiguration> mergedConfigs = mergeSavedConfigs(Arrays.asList(configs), savedConfigs, true);
			
			for (ColumnConfiguration config : mergedConfigs) {
				newColumn(config);
			}
			
			final int[] columnOrder = mergeSavedOrder(mergedConfigs, savedConfigs);
			if (columnOrder != null) {
				getTable().setColumnOrder(columnOrder);
			}
			
			updateSearch();
		}
		finally {
			getTable().setRedraw(true);
		}
		if (hadColumns) refresh();
	}
	
	private void reload(final List<ColumnConfiguration> savedConfigs) {
		getTable().setRedraw(false);
		try {
			boolean refresh = false;
			
			final List<ColumnConfiguration> mergedConfigs = mergeSavedConfigs(
					getCurrentConfigs(false, true), savedConfigs, false );
			
			final TableColumn[] existingColumns = getTable().getColumns();
			int existingIndex = 0;
			ITER_CONFIGS: for (final ColumnConfiguration config : mergedConfigs) {
				ITER_EXISTING: while (existingIndex < existingColumns.length) {
					final TableColumn column = existingColumns[existingIndex++];
					if (column.getData() != config) {
						removeColumn(column);
						continue ITER_EXISTING;
					}
					else {
						refresh |= applyConfig(column);
						continue ITER_CONFIGS;
					}
				}
				// else
				newColumn(config);
				continue ITER_CONFIGS;
			}
			while (existingIndex < existingColumns.length) {
				final TableColumn column = existingColumns[existingIndex++];
				removeColumn(column);
			}
			
			final int[] columnOrder = mergeSavedOrder(mergedConfigs, savedConfigs);
			if (columnOrder != null) {
				getTable().setColumnOrder(columnOrder);
			}
			
			if (refresh) {
				refresh();
			}
			
			updateSearch();
		}
		finally {
			getTable().setRedraw(true);
		}
	}
	
	
	public TableColumn addColumn(final ColumnConfiguration config) {
		cancelEditing();
		getTable().setRedraw(false);
		try {
			final TableColumn column = newColumn(config);
			refresh();
			
			return column;
		}
		finally {
			getTable().setRedraw(true);
		}
	}
	
	public void updateColumn(final TableColumn column) {
		updateColumns(Collections.singletonList(column));
	}
	
	public void updateColumns(final List<TableColumn> columns) {
		cancelEditing();
		getTable().setRedraw(false);
		try {
			boolean refresh = false;
			for (final TableColumn column : columns) {
				refresh |= applyConfig(column);
			}
			if (refresh) {
				refresh();
			}
		}
		finally {
			getTable().setRedraw(true);
		}
	}
	
	public void deleteColumn(final TableColumn column) {
		cancelEditing();
		getTable().setRedraw(false);
		try {
			removeColumn(column);
		}
		finally {
			getTable().setRedraw(true);
		}
	}
	
	
	private TableColumn newColumn(final ColumnConfiguration config) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(this, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setData(config);
		if (column.getData(VIEWER_COLUMN_KEY) == null) {
			column.setData(VIEWER_COLUMN_KEY, viewerColumn);
		}
		applyConfig(viewerColumn, config);
		return column;
	}
	
	private boolean applyConfig(final TableViewerColumn viewerColumn, final ColumnConfiguration config) {
		boolean refresh = true;
		
		final TableColumn column = viewerColumn.getColumn();
		column.setText(config.getName());
		column.setToolTipText(config.getTooltip());
		
		column.setMoveable(config.isMovable());
		if (config.isHidden()) {
			column.setWidth(0);
			column.setResizable(false);
		}
		else {
			column.setWidth(config.getWidth());
			column.setResizable(true);
		}
		
		final CellLabelProvider configLabelProvider = config.getLabelProvider();
		CellLabelProvider labelProvider = getLabelProvider(getTable().indexOf(column));
		if (labelProvider == null || labelProvider != configLabelProvider) {
			labelProvider = configLabelProvider;
			if (labelProvider == null)
				labelProvider = new RichLabelProvider(config);
			viewerColumn.setLabelProvider(labelProvider);
			column.setData("Image", labelProvider instanceof ImageLabelProvider);
			refresh = true;
		}
		
		final Comparator<?> configComparator = config.getSortComparator();
		ColumnViewerSorter<?> sorter = (ColumnViewerSorter<?>)column.getData(SORTER_KEY);
		if (sorter != null && sorter.getColumnComparator() != configComparator) {
			sorter.dispose();
			sorter = null;
		}
		if (sorter == null && configComparator != null) {
			sorter = new ColumnViewerSorter<>(viewerColumn, configComparator);
			column.setData(SORTER_KEY, sorter);
			if (config.getSortDirection() != SWT.NONE && !config.isHidden()) {
				sorter.setSorter(config.getSortDirection());
			}
		}
		
		ColumnEditingFactory.apply(viewerColumn, config.getEditingConfig());
		return refresh;
	}
	
	private boolean applyConfig(final TableColumn column) {
		final ColumnConfiguration config = getConfig(column);
		final TableViewerColumn viewerColumn = (TableViewerColumn)column.getData(VIEWER_COLUMN_KEY);
		return applyConfig(viewerColumn, config);
	}
	
	private void removeColumn(final TableColumn column) {
		ColumnViewerSorter<?> sorter = (ColumnViewerSorter<?>)column.getData(SORTER_KEY);
		if (sorter != null) {
			sorter.dispose();
			sorter = null;
		}
		column.dispose();
	}
	
	private ColumnConfiguration updateConfigFromView(final ColumnConfiguration config, final TableColumn column) {
		if (column.getWidth() != 0) {
			config.setWidth(column.getWidth());
		}
		config.setHidden(column.getWidth() == 0);
		config.setSortDirection((column == getTable().getSortColumn()) ? getTable().getSortDirection() : 0);
		return config;
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
		List<ColumnConfiguration> currentConfigs = getCurrentConfigs(true, true);
		return currentConfigs.toArray(new ColumnConfiguration[currentConfigs.size()]);
	}
	
	public void saveColumnState(final boolean sync) {
		IStateStore store = getStateStore();
		if (store == null) return;
		try {
			ColumnConfiguration[] configs = getCurrentColumnState();
			store.saveState(configs, tableKey, this, (sync) ? 1 : 0);
		} catch (IOException e) {
			EclipseLog.warn("Failed to save column state", e, Activator.getDefault());
		}
	}
	
	private void onStateChanged(final IStateStore.StateChangedEvent event) {
		if (event.getDetail() == 1 && event.getSource() != this && !getTable().isDisposed()) {
			reload(event.getConfigs());
		}
	}

	public void setDefaultSearchColumn(String name) {
		if (searchBar != null) searchBar.setCurrentName(name);
	}

	public boolean isSearchable() {
		return searchFilter != null;
	}

	public boolean hasColumnDataType(DataType type) {
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
				saveColumnState(false);
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
	
	public void updateSearch() {
		if (searchBar == null) {
			return;
		}
		List<String> names = new ArrayList<String>();
		List<ColumnConfiguration> configs = getCurrentConfigs(true, false);
		for (ColumnConfiguration config : configs) {
			if (!config.isHidden()
					&& config.getLabelProvider() instanceof ColumnLabelProvider) {
				String name = config.getName();
				if (name != null && !name.isEmpty()) {
					names.add(name);
				}
			}
		}
		searchBar.setNames(names.toArray(new String[names.size()]));
		searchFilter.setColumns(configs.toArray(new ColumnConfiguration[configs.size()]));
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
	
	
	private static int indexOfConfig(final List<ColumnConfiguration> configs, final String key) {
		for (int i = 0; i < configs.size(); i++) {
			if (configs.get(i).getKey().equals(key)) {
				return i;
			}
		}
		return -1;
	}

	private List<ColumnConfiguration> mergeSavedConfigs(final List<ColumnConfiguration> configs, final List<ColumnConfiguration> savedConfigs,
			final boolean updateView) {
		if (savedConfigs == null || savedConfigs.isEmpty()) {
			return configs;
		}
		
		List<ColumnConfiguration> merged = new ArrayList<ColumnConfiguration>(Math.max(configs.size(), savedConfigs.size()));
		List<ColumnConfiguration> savedTodo = new ArrayList<ColumnConfiguration>(savedConfigs);
		for (final ColumnConfiguration config : configs) {
			int savedIndex = indexOfConfig(savedTodo, config.getKey());
			if (savedIndex >= 0) {
				ColumnConfiguration savedConfig = savedTodo.remove(savedIndex);
				if (config.isCustom()) {
					config.setName(savedConfig.getName());
					config.setTooltip(savedConfig.getTooltip());
				}
				if (updateView) {
					config.setWidth(savedConfig.getWidth());
					config.setSortDirection(savedConfig.getSortDirection());
				}
				config.setCustomData(savedConfig.getCustomData());
				if (this.customColumnSupport != null && this.customColumnSupport.isSupported(config)) {
					this.customColumnSupport.applyCustomData(config);
				}
				merged.add(config);
			}
			else if (!config.isCustom()) {
				merged.add(config);
			}
		}
		
		if (this.customColumnSupport != null && !savedTodo.isEmpty()) {
			savedTodo.sort(Comparator.comparing(ColumnConfiguration::getKey));
			for (final ColumnConfiguration savedConfig : savedTodo) {
				if (this.customColumnSupport.isSupported(savedConfig)) {
					final ColumnConfiguration config = new ColumnConfiguration(savedConfig);
					this.customColumnSupport.applyCustomData(config);
					merged.add(config);
				}
			}
		}
		
		return merged;
	}
	
	private int[] mergeSavedOrder(List<ColumnConfiguration> configs, List<ColumnConfiguration> savedConfigs) {
		if (savedConfigs == null || savedConfigs.isEmpty()) {
			return null;
		}
		
		final int[] order = new int[configs.size()];
		final boolean[] orgDone = new boolean[configs.size()];
		int index = 0;
		for (final ColumnConfiguration savedConfig : savedConfigs) {
			int orgIndex = indexOfConfig(configs, savedConfig.getKey());
			if (orgIndex >= 0) {
				order[index++] = orgIndex;
				orgDone[orgIndex] = true;
			}
		}
		for (int i = 0; i < order.length; i++) {
			if (!orgDone[i]) {
				order[index++] = i;
			}
		}
		return order;
	}
	
}
