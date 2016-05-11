package eu.openanalytics.phaedra.base.ui.nattable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultEditableRule;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.config.NullComparator;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.validate.DefaultDataValidator;
import org.eclipse.nebula.widgets.nattable.data.validate.IDataValidator;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.export.config.DefaultExportBindings;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByConfigAttributes;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
import org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer;
import org.eclipse.nebula.widgets.nattable.freeze.CompositeFreezeLayer;
import org.eclipse.nebula.widgets.nattable.freeze.command.IFreezeCommand;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultSummaryRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.FixedSummaryRowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.layer.GridLineCellLayerPainter;
import org.eclipse.nebula.widgets.nattable.resize.command.MultiColumnResizeCommand;
import org.eclipse.nebula.widgets.nattable.resize.event.ColumnResizeEvent;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.painter.SortableHeaderTextPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.summaryrow.FixedSummaryRowLayer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.swt.IFocusService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEventListener;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.command.DisplayColumnChooserCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.command.FastMultiColumnResizeCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.command.FastMultiRowResizeCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.command.FixedFreezeCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.command.TreeColumnExpandCollapseCommandHandler;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.ContextMenuBindingConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.EditConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraColumnHeaderStyleConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraFilterRowConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraNatTableStyleConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraRowHeaderStyleConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraRowStyleConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraSelectionStyleConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.configuration.style.PhaedraSummaryRowConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy.ExtendedGroupByColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy.ExtendedGroupByDataLayer;
import eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy.ExtendedGroupByHeaderMenuConfiguration;
import eu.openanalytics.phaedra.base.ui.nattable.layer.ColumnGroupByBodyLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NatTableToolTip;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.ILinkedColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.LinkedResizeSupport.IResizeCallback;
import eu.openanalytics.phaedra.base.ui.nattable.painter.CustomBorderLineDecorator;
import eu.openanalytics.phaedra.base.ui.nattable.selection.CachedSelectionModel;
import eu.openanalytics.phaedra.base.ui.nattable.selection.ISelectionTransformer;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionManager;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionManager.INatTableSelectionListener;
import eu.openanalytics.phaedra.base.ui.nattable.state.IStatePersister;
import eu.openanalytics.phaedra.base.ui.nattable.state.PersistentStateSupport;
import eu.openanalytics.phaedra.base.ui.nattable.summaryrow.AbstractStatsSummaryProvider;
import eu.openanalytics.phaedra.base.ui.nattable.summaryrow.StatsSummaryProvider;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.misc.ContextHelper;

/**
 * <p>
 * Factory for creating new {@link NatTable} instances.
 * </p><p>
 * The table's functionality can be configured using the make* and add* methods.
 * When configuration is ready, the table can be created by calling {@link #build(Composite, boolean, MenuManager)}.
 * </p><p>
 * Afterwards, some additional objects (such as the table's {@link ISelectionProvider}) can be retrieved using
 * one of the available get* methods.
 * </p><p>
 * Note: this factory is NOT threadsafe.
 * </p>
 * 
 * @param <T> The type of object returned by the table's content provider.
 */
public class NatTableBuilder<T> {

	private static final String NO_SORT_LABEL = "NO_SORT_LABEL";

	private IColumnPropertyAccessor<T> columnAccessor;

	private GroupByHeaderLayer groupByHeaderLayer;
	private DataLayer bodyDataLayer;
	private ColumnGroupByBodyLayerStack bodyLayerStack;
	private FullFeaturedColumnHeaderLayerStack<T> columnHeaderLayer;
	private CompositeLayer compositeGridLayer;

	private ConfigRegistry configRegistry;
	private List<IConfiguration> customConfigurations;
	private List<INatTableSelectionListener> selectionListeners;

	private ColumnOverrideLabelAccumulator columnHeaderAccumulator;
	private ColumnOverrideLabelAccumulator columnBodyAccumulator;
	private LinkedResizeSupport linkedResizeSupport;
	private PersistentStateSupport persistentStateSupport;

	private FilterList<T> filterList;
	private SortedList<T> sortedList;
	private NatTableSelectionProvider<T> selectionProvider;
	private IRowDataProvider<T> dataProvider;

	private IDataValidator validator;
	private IEditableRule editableRule;

	private ISelectionTransformer<T> selectionTransformer;
	private List<Integer> hiddenColumnPositions;
	private int[] unsortableColumnIndexes;
	private int[] columnWidths;
	private boolean isGroupBy;
	private Map<int[], AbstractCellPainter> customCellPainters;
	private Map<String, IColumnMatcher> columnDialogMatchers;

	public NatTableBuilder(IColumnPropertyAccessor<T> columnAccessor, List<T> input) {
		this(columnAccessor, GlazedLists.eventList(input));
	}

	public NatTableBuilder(IColumnPropertyAccessor<T> columnAccessor, EventList<T> eventList) {
		this.filterList = new FilterList<>(eventList);
		this.sortedList = new SortedList<>(filterList, null);
		this.columnAccessor = columnAccessor;

		hiddenColumnPositions = new ArrayList<>();
		unsortableColumnIndexes = new int[0];
		columnWidths = new int[0];
		customCellPainters = new HashMap<>();
		columnDialogMatchers = new HashMap<>();

		configRegistry = new ConfigRegistry();
		customConfigurations = new ArrayList<>();
		selectionListeners = new ArrayList<>();
	}

	/**
	 * Make the cells in the table editable.
	 *
	 * @param validator Validates the new input.
	 * @param editableRule Decides which columns become editable.
	 * @return
	 */
	public NatTableBuilder<T> makeEditable(IDataValidator validator, IEditableRule editableRule) {
		this.validator = validator;
		this.editableRule = editableRule;
		return this;
	}

	/**
	 * Allow group by column on the table when set to <code>true</code>.
	 *
	 * Turning this feature on will make the table noticeably slower.
	 *
	 * @param isGroupBy <code>true</code> to add group by column support.
	 * @return
	 */
	public NatTableBuilder<T> makeGroupByable(boolean isGroupBy) {
		this.isGroupBy = isGroupBy;
		return this;
	}

	/**
	 * Make a column unsortable and unfilterable.
	 * @param columnIndexes
	 * @return
	 */
	public NatTableBuilder<T> makeUnsortable(int[] columnIndexes) {
		this.unsortableColumnIndexes = columnIndexes;
		return this;
	}

	/**
	 * Hide the given columns.
	 *
	 * @param columnPositions Positions for the columns which you would like to hide.
	 * @return
	 */
	public NatTableBuilder<T> hideColumns(Integer[] columnPositions) {
		return hideColumns(Arrays.asList(columnPositions));
	}

	/**
	 * Hide the given columns.
	 *
	 * @param columnPositions Positions for the columns which you would like to hide.
	 * @return
	 */
	public NatTableBuilder<T> hideColumns(List<Integer> columnPositions) {
		this.hiddenColumnPositions = columnPositions;
		return this;
	}

	/**
	 * Resize each column to the given size.
	 * For columns which you do not want to resize, use width -1.
	 *
	 * @param columnWidths
	 * @return
	 */
	public NatTableBuilder<T> resizeColumns(int[] columnWidths) {
		this.columnWidths = columnWidths;
		return this;
	}

	public NatTableBuilder<T> addSortedListEventListener(ListEventListener<T> listener) {
		sortedList.addListEventListener(listener);
		return this;
	}

	public NatTableBuilder<T> addSelectionProvider(ISelectionTransformer<T> transformer) {
		selectionTransformer = transformer;
		return this;
	}

	public NatTableBuilder<T> addSelectionListener(INatTableSelectionListener listener) {
		selectionListeners.add(listener);
		return this;
	}

	public NatTableBuilder<T> addConfiguration(IConfiguration config) {
		customConfigurations.add(config);
		return this;
	}

	public NatTableBuilder<T> addLinkedResizeSupport(float aspectRatio, IResizeCallback resizeCallback, ILinkedColumnAccessor<T> columnAccessor) {
		linkedResizeSupport = new LinkedResizeSupport(aspectRatio, resizeCallback, columnAccessor);
		return this;
	}

	public NatTableBuilder<T> addPersistentStateSupport(String key, IStatePersister persister) {
		persistentStateSupport = new PersistentStateSupport(key, persister);
		return this;
	}

	public NatTableBuilder<T> addCustomCellPainters(Map<int[], AbstractCellPainter> painters) {
		this.customCellPainters = painters;
		return this;
	}

	public NatTableBuilder<T> addColumnDialogMatchers(Map<String, IColumnMatcher> columnDialogMatchers) {
		this.columnDialogMatchers = columnDialogMatchers;
		return this;
	}

	@SuppressWarnings("unchecked")
	public NatTable build(Composite parent, boolean hasTooltips, MenuManager menuMgr) {
		// Body layer
		if (isGroupBy) {
			// Wrap the columnAccessor in a ExtendedGroupByColumnAccessor which adds a Group By column.
			columnAccessor = new ExtendedGroupByColumnAccessor<T>(columnAccessor);
			GroupByModel groupByModel = new GroupByModel();
			ExtendedGroupByDataLayer<T> groupByDataLayer = new ExtendedGroupByDataLayer<T>(groupByModel, sortedList, columnAccessor, configRegistry, true);
			// Update columnAccessor to the GroupByColumnAccessor.
			dataProvider = (IRowDataProvider<T>) groupByDataLayer.getDataProvider();
			bodyDataLayer = groupByDataLayer;

			GridLayer gridLayer = createGridLayer((EventList<T>) groupByDataLayer.getTreeList(), (GlazedListTreeRowModel<T>) groupByDataLayer.getTreeRowModel());

			// Shows the summary row at the bottom of the table.
			FixedSummaryRowLayer summaryRowLayer = new FixedSummaryRowLayer(groupByDataLayer, gridLayer, configRegistry, false) {
				@Override
				public String getSummaryRowLabel() {
					return AbstractStatsSummaryProvider.getSummaryMode().toString();
				}
			};

			// Ensure the body data layer uses a layer painter with correct configured clipping.
			groupByDataLayer.setLayerPainter(new GridLineCellLayerPainter(false, true));

			compositeGridLayer = new CompositeLayer(1, 3);
			groupByHeaderLayer = new GroupByHeaderLayer(groupByModel, gridLayer, columnHeaderLayer.getColumnHeaderDataProvider());
			compositeGridLayer.setChildLayer(GroupByHeaderLayer.GROUP_BY_REGION, groupByHeaderLayer, 0, 0);
			compositeGridLayer.setChildLayer("Grid", gridLayer, 0, 1);
			compositeGridLayer.setChildLayer(FixedSummaryRowHeaderLayer.DEFAULT_SUMMARY_ROW_LABEL, summaryRowLayer, 0, 2);

			// Add custom Collapse/Expand handler.
			groupByDataLayer.registerCommandHandler(new TreeColumnExpandCollapseCommandHandler(groupByDataLayer));
			// Add sorting support for tree.
			groupByDataLayer.initializeTreeComparator(
					columnHeaderLayer.getSortableColumnHeaderLayer().getSortModel()
					, bodyLayerStack.getTreeLayer(), true);

			bodyLayerStack.getColumnReorderLayer().reorderColumnPosition(columnAccessor.getColumnCount()-1, 0);

			configRegistry.registerConfigAttribute(GroupByConfigAttributes.GROUP_BY_CHILD_COUNT_PATTERN, "[{0}] - ({1})");
		} else {
			// Keep using the default columnAccessor.
			dataProvider = new GlazedListsDataProvider<T>(sortedList, columnAccessor);
			bodyDataLayer = new DataLayer(dataProvider);
			compositeGridLayer = createGridLayer(sortedList, null);
		}

		NatTable table = new NatTable(parent, compositeGridLayer, false);
		table.setConfigRegistry(configRegistry);

		if (hasTooltips) {
			// Add tooltips to both cells and column headers.
			new NatTableToolTip<T>(table, columnAccessor, dataProvider);
		}

		if (menuMgr != null) {
			Menu menu = menuMgr.createContextMenu(table);
			table.setMenu(menu);

			addCopyPasteLogic(table);
		}

		// Only needed if it supports GroupBy.
		if (isGroupBy) {
			// Create IRowDataProvider based on the sorted/filtered list which is needed for the summary row.
			// When GroupBy is enabled, it would wrap the sorted list in a TreeList and give incorrect results when a tree is collapsed.
			// Will be added as data to the table variable.
			IRowDataProvider<T> baseDataProvider = new ListDataProvider<T>(sortedList, columnAccessor);
			table.setData(StatsSummaryProvider.SUMMARY_DATAPROVIDER, baseDataProvider);

			table.addConfiguration(new ExtendedGroupByHeaderMenuConfiguration(table, groupByHeaderLayer));
		}

		// If table wasn't made editable, still add EditConfiguration so Filtering works.
		boolean editable = validator != null && editableRule != null;
		if (validator == null) validator = new DefaultDataValidator();
		if (editableRule == null) editableRule = new DefaultEditableRule(false);
		compositeGridLayer.addConfiguration(new EditConfiguration(validator, editableRule));

		table.addConfiguration(new ContextMenuBindingConfiguration(table, menuMgr, editable));
		table.addConfiguration(new SingleClickSortConfiguration(
				new CustomBorderLineDecorator(new SortableHeaderTextPainter(new TextPainter()))));
		table.addConfiguration(new DefaultExportBindings());

		// Add all style configurations to NatTable
		table.addConfiguration(new PhaedraNatTableStyleConfiguration());
		table.addConfiguration(new PhaedraColumnHeaderStyleConfiguration());
		table.addConfiguration(new PhaedraRowHeaderStyleConfiguration());
		table.addConfiguration(new PhaedraRowStyleConfiguration());
		table.addConfiguration(new PhaedraSelectionStyleConfiguration());
		table.addConfiguration(new PhaedraFilterRowConfiguration());
		table.addConfiguration(new PhaedraSummaryRowConfiguration());

		for (IConfiguration customConfig: customConfigurations) {
			table.addConfiguration(customConfig);
		}

		table.configure();

		if (!selectionListeners.isEmpty()) {
			NatTableSelectionManager mgr = new NatTableSelectionManager(bodyLayerStack.getSelectionLayer(), dataProvider);
			for (INatTableSelectionListener listener: selectionListeners) mgr.addSelectionListener(listener);
			table.addLayerListener(mgr);
		}

		if (linkedResizeSupport != null) linkedResizeSupport.apply(table, isGroupBy);
		if (persistentStateSupport != null) persistentStateSupport.apply(table, columnAccessor);

		return table;
	}

	/*
	 * Getters
	 *
	 * Because some objects used in the construction of the table are needed by
	 * the container for e.g. passing on selection or adding static filters we offer them here.
	 */

	/**
	 * @return The ColumnHeaderLayer that was used for the table.
	 */
	public FullFeaturedColumnHeaderLayerStack<T> getColumnHeaderLayer() {
		return columnHeaderLayer;
	}

	/**
	 * @return Returns the selection provider that was used (can be null if no selection transformer was given).
	 */
	public NatTableSelectionProvider<T> getSelectionProvider() {
		return selectionProvider;
	}

	private GridLayer createGridLayer(EventList<T> eventList, GlazedListTreeRowModel<T> treeRowModel) {

		GlazedListsEventLayer<T> glazedListEventLayer = new GlazedListsEventLayer<>(bodyDataLayer, eventList);
		ColumnGroupModel groupingModel = new ColumnGroupModel();
		bodyLayerStack = new ColumnGroupByBodyLayerStack(glazedListEventLayer, groupingModel, treeRowModel, columnAccessor.getColumnCount());
		SelectionLayer selectionLayer = bodyLayerStack.getSelectionLayer();
		CompositeFreezeLayer bodyLayer = new CompositeFreezeLayer(bodyLayerStack.getFreezeLayer(), bodyLayerStack.getViewportLayer(), selectionLayer);

		bodyDataLayer.unregisterCommandHandler(MultiColumnResizeCommand.class);
		bodyDataLayer.registerCommandHandler(new FastMultiColumnResizeCommandHandler(bodyDataLayer));
		bodyDataLayer.registerCommandHandler(new FastMultiRowResizeCommandHandler(bodyDataLayer));
		bodyLayer.unregisterCommandHandler(IFreezeCommand.class);
		bodyLayer.registerCommandHandler(new FixedFreezeCommandHandler(bodyLayerStack.getFreezeLayer(), bodyLayerStack.getViewportLayer(), selectionLayer));

		// Column layer
		columnHeaderLayer = new FullFeaturedColumnHeaderLayerStack<>(
				columnAccessor
				, sortedList
				, filterList
				, bodyLayer
				, selectionLayer
				, groupingModel
				, configRegistry
		);

		columnHeaderAccumulator = applyLabelAccumulator(columnHeaderLayer.getColumnHeaderDataLayer());
		columnBodyAccumulator = applyLabelAccumulator(bodyDataLayer);

		// Row layer
		DefaultRowHeaderDataProvider rowHeaderDataProvider = null;
		if (isGroupBy) {
			rowHeaderDataProvider = new DefaultSummaryRowHeaderDataProvider(dataProvider, "\u2211");
			rowHeaderDataProvider = new DefaultRowHeaderDataProvider(rowHeaderDataProvider);
		} else {
			rowHeaderDataProvider = new DefaultRowHeaderDataProvider(dataProvider);
		}
		DataLayer rowDataLayer = new DataLayer(rowHeaderDataProvider, 52, DataLayer.DEFAULT_ROW_HEIGHT);
		RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowDataLayer, bodyLayer, selectionLayer);
		// Increase the width of the Row Number column. Width 52 supports up to 9.999.999 rows and the MEDIAN text for Summary.
		rowDataLayer.setColumnPositionResizable(0, false);

		// Column Chooser
		DisplayColumnChooserCommandHandler columnChooserCommandHandler = new DisplayColumnChooserCommandHandler(
				selectionLayer
				, bodyLayerStack.getColumnHideShowLayer()
				, columnHeaderLayer.getColumnHeaderLayer()
				, columnHeaderLayer.getColumnHeaderDataLayer()
				, columnHeaderLayer.getColumnGroupHeaderLayer()
				, groupingModel
				, columnDialogMatchers
		);
		bodyLayer.registerCommandHandler(columnChooserCommandHandler);

		// Corner Layer
		DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(columnHeaderLayer.getColumnHeaderDataProvider(), rowHeaderDataProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		CornerLayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);

		GridLayer gridLayer = new GridLayer(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer, false);

		bodyLayerStack.getColumnHideShowLayer().hideColumnPositions(hiddenColumnPositions);

		for (int column = 0; column < columnWidths.length; column++) {
			int width = columnWidths[column];
			if (width != -1) bodyDataLayer.setColumnWidthByPosition(column, width, false);
		}
		bodyDataLayer.fireLayerEvent(new ColumnResizeEvent(bodyDataLayer, new Range(0, columnWidths.length)));

		for (int[] columns: customCellPainters.keySet()) {
			AbstractCellPainter painter = customCellPainters.get(columns);
			String label = UUID.randomUUID().toString();
			addConfiguration(new AbstractRegistryConfiguration() {
				@Override
				public void configureRegistry(IConfigRegistry configRegistry) {
					configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, painter, DisplayMode.NORMAL, label);
				}
			});
			for (int c: columns) {
				columnBodyAccumulator.registerColumnOverrides(c, label);
			}
		}

		for (int c : unsortableColumnIndexes) {
			columnHeaderAccumulator.registerColumnOverrides(c, NO_SORT_LABEL);
			// Make unfilterable.
			configRegistry.registerConfigAttribute(
					EditConfigAttributes.CELL_EDITABLE_RULE
					, IEditableRule.NEVER_EDITABLE
					, DisplayMode.NORMAL
					, FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX + c);
		}

		configRegistry.registerConfigAttribute(
				SortConfigAttributes.SORT_COMPARATOR,
                new NullComparator(),
                DisplayMode.NORMAL,
                NO_SORT_LABEL
        );

		if (selectionTransformer != null) {
			selectionProvider = new NatTableSelectionProvider<>(columnHeaderLayer, dataProvider, false, false, selectionTransformer);
			// Allow multiple selections.
			selectionProvider.setAddSelectionOnSet(true);
			if (selectionLayer.getSelectionModel() instanceof CachedSelectionModel) {
				// Use custom selection event handler.
				((CachedSelectionModel) selectionLayer.getSelectionModel()).setSelectionProvider(selectionProvider);
			}
		}

		return gridLayer;
	}

	private ColumnOverrideLabelAccumulator applyLabelAccumulator(DataLayer dataLayer) {
		// This accumulator will add the column name to all the cells of a column. Useful for adding DisplayConverters per column.
		ColumnOverrideLabelAccumulator bodyLabelAccumulator = new ColumnOverrideLabelAccumulator(dataLayer) {
			@Override
			public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
				int columnIndex = dataLayer.getColumnIndexByPosition(columnPosition);
				if (columnIndex >= 0) configLabels.addLabel(columnAccessor.getColumnProperty(columnIndex));
				List<String> overrides = getOverrides(Integer.valueOf(columnIndex));
				if (overrides != null) {
					for (String configLabel : overrides) {
						configLabels.addLabel(configLabel);
					}
				}
			}
		};
		dataLayer.setConfigLabelAccumulator(bodyLabelAccumulator);
		return bodyLabelAccumulator;
	}

	private void addCopyPasteLogic(NatTable table) {
		// Track focus of table, this is required for command handlers that want to know which table triggered the command.
		IFocusService service = (IFocusService) PlatformUI.getWorkbench().getService(IFocusService.class);
		service.addFocusTracker(table, "natTable");
		table.addDisposeListener(e -> service.removeFocusTracker(table));

		// Add copy-paste context.
		ContextHelper.attachContext(table, CopyItems.COPY_PASTE_CONTEXT_ID);
	}

}