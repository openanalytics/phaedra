package eu.openanalytics.phaedra.ui.curve.grid;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.MultiColumnShowCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ShowAllColumnsCommand;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.resize.command.InitializeAutoResizeColumnsCommand;
import org.eclipse.nebula.widgets.nattable.util.GCFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.layer.FullFeaturedColumnHeaderLayerStack;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.curve.util.ConcentrationFormat;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundContentProvider;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundSelectionTransformer;
import eu.openanalytics.phaedra.ui.curve.grid.tooltip.CompoundToolTip;

public class CompoundGrid extends Composite {

	private static final String[] compoundListCurveSizes = new String[] { "100 x 100", "150 x 150", "200 x 200", "250 x 250" };

	private NatTable table;
	private FullFeaturedColumnHeaderLayerStack<Compound> columnHeaderLayer;
	private NatTableSelectionProvider<Compound> selectionProvider;
	private CompoundContentProvider columnAccessor;
	private Matcher<Compound> selectedMatcher;

	private boolean isShowSelectedOnly;

	private enum FilterState {
		SHOW_ALL, CURVES_ONLY, HIDE_CURVES, SHOW_STRUCTURES, HIDE_STRUCTURES
	};

	public CompoundGrid(Composite parent, List<Compound> compounds, List<Feature> features, MenuManager menuMgr) {
		super(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(this);

		columnAccessor = new CompoundContentProvider(compounds, features);

		NatTableBuilder<Compound> builder = new NatTableBuilder<>(columnAccessor, compounds);
		table = builder
				.resizeColumns(columnAccessor.getColumnWidths())
				.hideColumns(columnAccessor.getDefaultHiddenColumns())
				.addCustomCellPainters(columnAccessor.getCustomCellPainters())
				.addConfiguration(columnAccessor.getCustomConfiguration())
				.addSelectionProvider(new CompoundSelectionTransformer(columnAccessor))
				.build(this, false, menuMgr);
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(table);

		columnHeaderLayer = builder.getColumnHeaderLayer();
		selectionProvider = builder.getSelectionProvider();

		new CompoundToolTip(table, columnAccessor, selectionProvider.getRowDataProvider());

		selectedMatcher = row -> selectionProvider.getCurrentListselection().contains(row);

		GridColumnGroup[] groups = columnAccessor.getGroups();
		for (GridColumnGroup group: groups) {
			String name = group.getGroupName();
			int[] columns = group.getGroupColumns();
			columnHeaderLayer.getColumnGroupHeaderLayer().addColumnsIndexesToGroup(name, columns);
		}

		NatTableUtils.resizeAllRows(table, 100);

		columnAccessor.preLoad(table);
	}

	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	public CompoundContentProvider getCompoundContentProvider() {
		return columnAccessor;
	}

	public NatTable getTable() {
		return table;
	}

	@Override
	public void dispose() {
		table.dispose();
		super.dispose();
	}

	@Override
	public void redraw() {
		table.doCommand(new VisualRefreshCommand());
		super.redraw();
	}

	public void setCurveSize(int x, int y) {
		columnAccessor.setCurveSize(x, y);
		setGridCellWidth(x);
		setGridCellHeight(y);
		columnAccessor.preLoad(table);
	}

	public void createButtons(ToolBar toolbar) {
		DropDown item = new DropDown(toolbar, null, IconManager.getIconImage("curve.png"));
		item.setTooltipText("Filter Columns");
		item.setItemSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem item = (MenuItem) e.widget;
				if (item.getSelection()) {
					FilterState state = (FilterState) item.getData();

					// See if the Structure Column was hidden.
					int structColIndex = columnAccessor.getStructureColumn();
					int structColPos = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(structColIndex);
					boolean isStructHidden = structColPos < 0;

					// First, show all to undo previous filter state
					table.doCommand(new ShowAllColumnsCommand());
					setGridCellWidth(columnAccessor.getCurveWidth());
					setGridCellHeight(columnAccessor.getCurveHeight());

					if (isStructHidden) {
						// Structure was hidden, keep it that way.
						NatTableUtils.hideColumn(table, structColIndex);
					}

					switch (state) {
					case CURVES_ONLY:
						int[] indices = columnAccessor.getCurveColumns();
						int colCount = columnAccessor.getColumnCount();
						int[] colIndexes = new int[colCount - indices.length];
						int index = 0, indexToHide = 0;
						for (int i = 0; i < colCount; i++) {
							if (CollectionUtils.find(indices, i) == -1) {
								if (i == 5 || i == 6 || i == 7) {
									// Compound nr, saltform: do not hide.
								} else {
									// Not a curve column: hide it.
									colIndexes[index++] = indexToHide;
								}

							}
							indexToHide++;
						}
						NatTableUtils.hideColumns(table, colIndexes);
						break;
					case HIDE_CURVES:
						colIndexes = columnAccessor.getCurveColumns();
						NatTableUtils.hideColumns(table, colIndexes);
						if (isStructHidden) {
							setGridCellHeight(DataLayer.DEFAULT_ROW_HEIGHT);
						} else {
							// The structure is enabled and needs space.
							setGridCellHeight(100);
						}
						break;
					default:
						// Do nothing.
					}
				}
			}
		});

		item.addRadioItem("Show All Columns", null, FilterState.SHOW_ALL);
		item.addRadioItem("Curves Only", null, FilterState.CURVES_ONLY);
		item.addRadioItem("Hide Curves", null, FilterState.HIDE_CURVES);

		item = new DropDown(toolbar, null, IconManager.getIconImage("benzene.gif"));
		item.setTooltipText("Filter Smiles");
		item.setItemSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem item = (MenuItem) e.widget;
				if (item.getSelection()) {
					FilterState state = (FilterState) item.getData();
					int index = columnAccessor.getStructureColumn();
					switch (state) {
					case SHOW_STRUCTURES:
						table.doCommand(new MultiColumnShowCommand(Arrays.asList(index)));
						setGridCellWidth(columnAccessor.getCurveWidth());
						setGridCellHeight(columnAccessor.getCurveHeight());
						break;
					case HIDE_STRUCTURES:
						NatTableUtils.hideColumn(table, index);
						break;
					default:
						// Do nothing.
					}
				}
			}
		});

		int colIndex = columnHeaderLayer.getSelectionLayer().getColumnPositionByIndex(columnAccessor.getStructureColumn());
		item.addRadioItem("Show Structures", null, FilterState.SHOW_STRUCTURES);
		item.addRadioItem("Hide Structures", null, FilterState.HIDE_STRUCTURES);
		item.select(colIndex < 0 ? 1 : 0, false);

		item = new DropDown(toolbar, null, IconManager.getIconImage("channel_magnify.png"));
		item.setTooltipText("Curve Size");
		item.setItemSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem item = (MenuItem) e.widget;
				if (item.getSelection()) {
					String size = item.getText();
					int x = Integer.parseInt(size.split(" x ")[0]);
					int y = Integer.parseInt(size.split(" x ")[1]);
					setCurveSize(x, y);
				}
			}
		});

		String currentCurveSize = columnAccessor.getCurveWidth() + " x " + columnAccessor.getCurveHeight();
		for (int i = 0; i < compoundListCurveSizes.length; i++) {
			String curveSize = compoundListCurveSizes[i];
			item.addRadioItem(curveSize, null, null);
			if (curveSize.equals(currentCurveSize)) {
				item.select(i, true);
			}
		}

		ToolItem formatDropdown = DropdownToolItemFactory.createDropdown(toolbar);
		formatDropdown.setImage(IconManager.getIconImage("logM.png"));
		formatDropdown.setToolTipText("Concentration format");

		ConcentrationFormat[] formats = ConcentrationFormat.values();
		for (ConcentrationFormat format: formats) {
			MenuItem menuItem = DropdownToolItemFactory.createChild(formatDropdown, format.toString() + ": " + format.getLabel(), SWT.RADIO);
			menuItem.setData(format);
			menuItem.addListener(SWT.Selection, e -> {
				MenuItem selItem = (MenuItem)e.widget;
				if (!selItem.getSelection()) return;
				columnAccessor.setConcFormat((ConcentrationFormat)selItem.getData());
				table.doCommand(new VisualRefreshCommand());
			});
			menuItem.setSelection(format == columnAccessor.getConcFormat());
		}

		new ToolItem(toolbar, SWT.SEPARATOR);

		ToolItem selectedOnlyItem = new ToolItem(toolbar, SWT.CHECK);
		selectedOnlyItem.setImage(IconManager.getIconImage("wand.png"));
		selectedOnlyItem.setToolTipText("Show selected items / all items");
		selectedOnlyItem.addListener(SWT.Selection, e -> {
			isShowSelectedOnly = ((ToolItem) e.widget).getSelection();
			if (isShowSelectedOnly) {
				columnHeaderLayer.addStaticFilter(selectedMatcher);
			} else {
				columnHeaderLayer.removeStaticFilter(selectedMatcher);
			}
		});
		selectedOnlyItem.setSelection(isShowSelectedOnly);

		ToolItem toolItem = new ToolItem(toolbar, SWT.PUSH);
		toolItem.setImage(IconManager.getIconImage("select_column.png"));
		toolItem.setToolTipText("Auto Resize All Columns");
		toolItem.addListener(SWT.Selection, e -> {
			GCFactory gc = new GCFactory(table);
			int[] curveColumns = columnAccessor.getCurveColumns();
			for (int column = 1; column < columnAccessor.getColumnCount(); column++) {
				if (CollectionUtils.find(curveColumns, column - 1) == -1) {
					InitializeAutoResizeColumnsCommand command = new InitializeAutoResizeColumnsCommand(table, column, table.getConfigRegistry(), gc);
					table.doCommand(command);
				}
			}
		});
	}

	protected void preSelection() {
		if (isShowSelectedOnly) columnHeaderLayer.removeStaticFilter(selectedMatcher);
	}

	protected void postSelection() {
		if (isShowSelectedOnly) columnHeaderLayer.addStaticFilter(selectedMatcher);
	}

	private void setGridCellWidth(int width) {
		int[] colIndexes = columnAccessor.getCurveColumns();
		NatTableUtils.resizeColumns(table, colIndexes, width);
		NatTableUtils.resizeColumn(table, columnAccessor.getStructureColumn(), width);
	}

	private void setGridCellHeight(int height) {
		NatTableUtils.resizeAllRows(table, height);
	}

}
