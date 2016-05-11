package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.ACTIONS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.GROUPING;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NAME;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_AVAILABLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_SELECTED_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.NUMBER_VISIBLE_POINTS;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend.SELECTED_FEATURES;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.OPACITY;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem.SIZE;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Observer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegend;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractLegendItem;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.AggregationDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.AggregationMenuFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataCalculator;
import eu.openanalytics.phaedra.base.ui.charting.v2.filter.IFilter;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.CustomComboBoxCellEditor;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class BaseLegendView<ENTITY, ITEM> extends LayeredView<ENTITY, ITEM> {

	private static final String SUBLAYER = "SUBLAYER";
	private static final String LAYER = "LAYER";
	private Tree legendTree;
	private CheckboxTreeViewer legendViewer;
	private final List<TreeEditor> treeEditors = new ArrayList<TreeEditor>();
	private List<IGroupingStrategy<ENTITY, ITEM>> groupingStrategies;
	private String[] COLUMNS = new String[] { NAME, SIZE, OPACITY, GROUPING, NUMBER_AVAILABLE_POINTS,
			NUMBER_SELECTED_POINTS, NUMBER_VISIBLE_POINTS, SELECTED_FEATURES, ACTIONS };
	private TextCellEditor textCellEditor;
	private CustomComboBoxCellEditor groupingStrategiesCellEditor;
	private boolean lockedForRefresh;

	private Job refreshLegendJob;

	private ValueObservable activeLayerChangedObservable = new ValueObservable();
	private ValueObservable layerOrderChangedObservable = new ValueObservable();
	private ValueObservable layerGroupingChangedObservable = new ValueObservable();
	private ValueObservable layerStatusChangedObservable = new ValueObservable();
	private ValueObservable layerFilterChangedObservable = new ValueObservable();
	private ValueObservable layerFeatureChangedObservable = new ValueObservable();
	private ValueObservable layerRemovedObservable = new ValueObservable();
	private ValueObservable layerToggleObservable = new ValueObservable();

	public BaseLegendView(Composite parent, List<AbstractChartLayer<ENTITY, ITEM>> chartLayers,
			List<IGroupingStrategy<ENTITY, ITEM>> groupingStrategies) {
		super(chartLayers);
		this.groupingStrategies = groupingStrategies;

		legendTree = new Tree(parent, SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI
				| SWT.FULL_SELECTION);

		legendViewer = new CheckboxTreeViewer(legendTree);
		parent.addListener(SWT.Dispose, e -> {
			cleanupLegendModel(legendViewer.getInput());
			legendTree.dispose();
		});

		initializeTree();
		initializeLegendViewer();
		createRefreshLegendTreeJob();

		// Set the height correctly. Otherwise menubar isn't shown ...
		legendTree.addListener(SWT.MeasureItem, e -> e.height = 22);
	}

	/*
	 * Private
	 */

	private void initializeLegendViewer() {
		legendViewer.setContentProvider(new LegendContentProvider());
		legendViewer.setLabelProvider(new LegendLabelProvider());
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[] { TextTransfer.getInstance() };
		legendViewer.addDropSupport(operations, transferTypes, new ViewerDropAdapter(legendViewer) {
			@Override
			public boolean performDrop(Object data) {
				String sData = (String) data;
				if (!sData.startsWith(SUBLAYER)) {
					dropLayer(Integer.parseInt(sData.substring(LAYER.length())));
					getLayerOrderChangedObservable().valueChanged();

					refreshLegendTree();

					return true;
				}
				return false;
			}
			@Override
			public boolean validateDrop(Object target, int operation, TransferData type) {
				return target instanceof AbstractLegend;
			}

			private void dropLayer(int fromOrder) {
				if (getCurrentTarget() != null) {
					int toOrder;
					if (getCurrentTarget() instanceof AbstractLegend) {
						toOrder = ((AbstractLegend<?, ?>) getCurrentTarget()).getLayer().getOrder();
					} else {
						toOrder = ((AbstractLegendItem<?, ?>) getCurrentTarget()).getParent().getLayer().getOrder();
					}
					AbstractChartLayer<ENTITY, ITEM> selectionLayer = getSelectionLayer();
					int orderToCheck = selectionLayer != null ? getSelectionLayer().getOrder() - 1 : getChartLayers().size();
					if (LOCATION_AFTER == getCurrentLocation() && toOrder < orderToCheck) {
						toOrder++;
					}

					AbstractChartLayer<ENTITY, ITEM> movingLayer = getChartLayers().get(fromOrder - 1);
					getChartLayers().remove(movingLayer);
					getChartLayers().add(toOrder - 1, movingLayer);
					// re-order the layers
					int order = 1;
					for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
						layer.setOrder(order++);
					}
				}
			}
		});

		legendViewer.addDragSupport(operations, transferTypes, new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				// Only allow AbstractLegend Drag & Drops
				IStructuredSelection selection = (IStructuredSelection) legendViewer.getSelection();
				if (!selection.isEmpty()) {
					if (selection.getFirstElement() instanceof AbstractLegend) {
						return;
					}
				}
				event.detail = DND.DROP_NONE;
				event.doit = false;
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) legendViewer.getSelection();
				String order = "";
				if (selection.getFirstElement() instanceof AbstractLegend) {
					AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) selection.getFirstElement();
					order = LAYER + legend.getLayer().getOrder();
				} else {
					AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) selection.getFirstElement();
					order = SUBLAYER + legendItem.getOrder();
				}

				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = order;
				}
			}
		});

		legendViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) {
				if (element instanceof AbstractLegendItem) {
					return ((AbstractLegendItem<?, ?>) element).isEnabled();
				} else if (element instanceof AbstractLegend) {
					return ((AbstractLegend<?, ?>) element).getLayer().isEnabled();
				}
				return false;
			}

			@Override
			public boolean isGrayed(Object element) {
				return false;
			}
		});

		updateLayers();
	}

	private void initializeTree() {
		legendTree.setHeaderVisible(true);
		textCellEditor = new TextCellEditor(legendTree);
		groupingStrategiesCellEditor = new CustomComboBoxCellEditor(legendViewer.getTree(), getGroupingStrategyLabels());

		TreeViewerColumn col = new TreeViewerColumn(legendViewer, SWT.LEFT);
		col.getColumn().setWidth(125);
		col.getColumn().setText(NAME);

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(40);
		col.getColumn().setText(SIZE);
		col.setEditingSupport(getSizeEditingSupport());

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(90);
		col.getColumn().setText(OPACITY);
		col.setEditingSupport(getOpacityEditingSupport());

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(100);
		col.getColumn().setText(GROUPING);
		col.setEditingSupport(getGroupEditingSupport());

		addSpecificColumns();

		// Stats
		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(40);
		col.getColumn().setText(NUMBER_AVAILABLE_POINTS);

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(60);
		col.getColumn().setText(NUMBER_SELECTED_POINTS);

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(50);
		col.getColumn().setText(NUMBER_VISIBLE_POINTS);

		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(120);
		col.getColumn().setText(SELECTED_FEATURES);

		// ACTIONS
		col = new TreeViewerColumn(legendViewer, SWT.CENTER);
		col.getColumn().setWidth(250);
		col.getColumn().setText(ACTIONS);
		col.getColumn().setResizable(false);

		legendTree.addTreeListener(new TreeAdapter() {
			@Override
			public void treeExpanded(TreeEvent e) {
				TreeItem item = ((TreeItem) e.item);
				if (item.getData() instanceof AbstractLegend) {
					// Refresh the tree, otherwise components such as aux axis legend will not be drawn.
					createEditors();
				}
			}
		});
		legendTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem item = ((TreeItem) e.item);
				Object object = item.getData();
				if (e.detail == SWT.CHECK) {
					// The Checkbox was selected. Enable/Disable Layer/Group of Layer.
					AbstractChartLayer<?, ?> layer = null;
					boolean enabled = item.getChecked();
					if (object instanceof AbstractLegend) {
						AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) object;
						layer = legend.getLayer();
						layer.setEnabled(enabled);
						getLayerToggleObservable().valueChanged(layer);
					} else if (object instanceof AbstractLegendItem) {
						AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) object;
						layer = legendItem.getParent().getLayer();
						if (legendItem.getStyle() != null ) {
							legendItem.setEnabled(!enabled);
							getLayerToggleObservable().valueChanged(layer);
						} else {
							// Fix: disable hiding of legend items that have no Style object.
							e.detail = SWT.NONE;
							e.doit = false;
							try {
								legendTree.setRedraw(false);
								item.setChecked(true);
							} finally {
								legendTree.setRedraw(true);
							}
						}
					}
				} else {
					// Mark the selected layer as active layer.
					if (object instanceof AbstractLegend) {
						AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) object;
						getActiveLayerChangedObservable().valueChanged(legend.getLayer());
					}
					// Highlight the selected group (Select it).
					if (object instanceof AbstractLegendItem) {
						// Check if the mouse clicked on the name of the TreeItem
						Point cursorPos = legendTree.toControl(Display.getDefault().getCursorLocation());
						Rectangle textBounds = item.getTextBounds(0);
						if (cursorPos.x > textBounds.x && cursorPos.x < (textBounds.x + textBounds.width)) {
							// Get legend
							AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) object;
							AbstractChartLayer<?, ?> layer = legendItem.getParent().getLayer();
							BitSet bitSet = new BitSet();
							for (TreeItem selection : legendTree.getSelection()) {
								// Update BitSet
								BitSet tempSet = layer.getDataProvider().getActiveGroupingStrategy().getGroups().get(selection.getText());
								if (tempSet != null) {
									bitSet.or(tempSet);
								}
							}
							// Apply new selection
							layer.getChart().setSelection(bitSet);
							getLayerToggleObservable().valueChanged(layer);
						}
					}
				}
			}
		});
	}

	private EditingSupport getSizeEditingSupport() {
		return new EditingSupport(legendViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractLegendItem) {
					AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) element;
					// Only change when the property was modified
					if (!legendItem.getPropertyValue(SIZE).equalsIgnoreCase(String.valueOf(value))) {
						legendItem.setPropertyValue(SIZE, String.valueOf(value));
						getLayerStatusChangedObservable().valueChanged(legendItem.getParent().getLayer());
						refreshLegendTree();
					}
				}
			}
			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractLegendItem) {
					AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) element;
					return legendItem.getPropertyValue(SIZE);
				}
				return null;
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return textCellEditor;
			}
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof AbstractLegendItem) {
					return ((AbstractLegendItem<?, ?>) element).canModify(SIZE);
				}
				return false;
			}
		};
	}

	private EditingSupport getOpacityEditingSupport() {
		return new EditingSupport(legendViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractLegendItem) {
					AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) element;
					// Only change when the property was modified
					if (!legendItem.getPropertyValue(OPACITY).equalsIgnoreCase(String.valueOf(value))) {
						legendItem.setPropertyValue(OPACITY, String.valueOf(value));
						getLayerStatusChangedObservable().valueChanged(legendItem.getParent().getLayer());
						refreshLegendTree();
					}
				}
			}
			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractLegendItem) {
					AbstractLegendItem<?, ?> legendItem = (AbstractLegendItem<?, ?>) element;
					return legendItem.getPropertyValue(OPACITY);
				}
				return null;
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return textCellEditor;
			}
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof AbstractLegendItem) {
					return ((AbstractLegendItem<?, ?>) element).canModify(OPACITY);
				}
				return false;
			}
		};
	}

	private EditingSupport getGroupEditingSupport() {
		return new EditingSupport(legendViewer) {
			@SuppressWarnings("unchecked")
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractLegend) {
					AbstractLegend<ENTITY, ITEM> legend = (AbstractLegend<ENTITY, ITEM>) element;
					AbstractChartLayer<ENTITY, ITEM> layer = legend.getLayer();
					changeActiveGrouping(Integer.parseInt(String.valueOf(value)), layer);
				}
				refreshLegendTree();
			}
			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractLegend) {
					AbstractLegend<?, ?> legend = (AbstractLegend<?, ?>) element;
					String currentStrategy = legend.getDataProvider().getActiveGroupingStrategy().getName();
					String[] groupingLabels = getGroupingStrategyLabels();
					for (int i = 0; i < groupingLabels.length; i++) {
						if (groupingLabels[i].equals(currentStrategy)) {
							return i;
						}
					}
					return 0;
				}
				return null;
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return groupingStrategiesCellEditor;
			}
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof AbstractLegend) {
					return ((AbstractLegend<?, ?>) element).canModify(GROUPING);
				}
				return false;
			}
		};
	}

	private class LegendLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			String text = "";
			Object element = cell.getElement();

			if (element instanceof AbstractLegend) {
				AbstractLegend<?, ?> item = (AbstractLegend<?, ?>) element;
				text = item.getPropertyValue(getColumnName(cell.getColumnIndex()));
			} else if (element instanceof AbstractLegendItem) {
				AbstractLegendItem<?, ?> item = (AbstractLegendItem<?, ?>) element;
				text = item.getPropertyValue(getColumnName(cell.getColumnIndex()));
				if (cell.getColumnIndex() == 0) {
					cell.setImage(item.getIconImage());
				}
			}
			cell.setText(text);
			super.update(cell);
		}
	}

	@SuppressWarnings("unchecked")
	private void cleanupLegendModel(Object legendModel) {
		if (legendModel instanceof List) {
			List<AbstractLegend<ENTITY, ITEM>> model = (List<AbstractLegend<ENTITY, ITEM>>) legendModel;
			for (AbstractLegend<ENTITY, ITEM> legend : model) {
				if (legend.getLegendItems() != null) {
					for (AbstractLegendItem<ENTITY, ITEM> legendItem : legend.getLegendItems()) {
						legendItem.disposeImages();
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void createEditors() {
		for (TreeEditor editor : treeEditors) {
			if (editor != null) {
				if (editor.getEditor() != null) {
					editor.getEditor().dispose();
				}
				editor.dispose();
			}
		}
		treeEditors.clear();

		for (final TreeItem item : legendTree.getItems()) {
			AbstractLegend<ENTITY, ITEM> legend = (AbstractLegend<ENTITY, ITEM>) item.getData();

			treeEditors.add(createLegendEditor(item, legend.getLayer()));

			for (int i = 0; i < item.getItemCount(); i++) {
				final TreeItem treeItem = item.getItem(i);
				if (item != null) {
					AbstractLegendItem<ENTITY, ITEM> legendItem = legend.getLegendItems().get(i);
					if (legendItem != null && legendItem.hasAuxilaryData()) {
						treeEditors.add(createLegendItemEditor(treeItem, legendItem, legend.getLayer()));
					}
				}
			}
		}
	}

	/**
	 * create legend item editor for e.g. auxilary axes, density axes, etc
	 */
	private TreeEditor createLegendItemEditor(final TreeItem treeItem, final AbstractLegendItem<ENTITY, ITEM> legendItem,
			final AbstractChartLayer<ENTITY, ITEM> layer) {
		TreeEditor editor = new TreeEditor(legendTree);
		editor.grabHorizontal = true;
		int width = 0;
		for (int i = 0; i < legendTree.getColumnCount(); i++) {
			width += legendTree.getColumn(i).getWidth();
		}
		editor.minimumWidth = width - legendTree.getColumn(0).getWidth();

		final Label label = new Label(legendTree, SWT.HORIZONTAL);
		label.setImage(legendItem.getAuxilaryImage(editor.minimumWidth, 22));
		label.addPaintListener(legendItem.getPaintListener());
		label.addMouseListener(legendItem.getMouseListener(this));
		label.addMouseMoveListener(legendItem.getMouseMoveListener());

		editor.setEditor(label, treeItem, 1);
		return editor;
	}

	private TreeEditor createLegendEditor(TreeItem item, final AbstractChartLayer<ENTITY, ITEM> layer) {
		ToolBar toolBar = new ToolBar(legendTree, SWT.HORIZONTAL);
		toolBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		// configuration button
		ToolItem configButton = new ToolItem(toolBar, SWT.PUSH);
		configButton.setImage(IconManager.getIconImage("book_edit.png"));
		configButton.setToolTipText("Adjust settings");
		configButton.addListener(SWT.Selection, e -> {
			layer.getLegend().showSettingsDialog(legendTree.getShell(), getLayerFilterChangedObservable());
		});

		if (layer.getLegend().hasAxesSupport()) {
			createAxesButtons(toolBar, layer);
		}

		// Filter and Aggregation buttons
		if (layer.getLegend().isFilterable()) {
			createFilterMenuButtons(toolBar, layer);
			AggregationMenuFactory.createFor(toolBar, layer, getLayerFilterChangedObservable());
		}

		// Aux legends button
		if (layer.getLegend().isShowAuxilaryAxes()) {
			ToolItem auxilaryAxesButton = new ToolItem(toolBar, SWT.PUSH);
			auxilaryAxesButton.setImage(IconManager.getIconImage("acolour1.png"));
			auxilaryAxesButton.setToolTipText("Activate color axes");
			auxilaryAxesButton.addListener(SWT.Selection, e -> {
				if (layer.getDataProvider().getActiveGroupingStrategy() != null
						&& !DefaultGroupingStrategy.DEFAULT_GROUPING_NAME.equals(layer.getDataProvider().getActiveGroupingStrategy().getName())) {
					MessageDialog.openError(legendTree.getShell(), "Error", "Can only show auxiliary axes when grouping mode is set to All.");
				} else if (layer.getChartSettings().isLines()) {
					MessageDialog.openError(legendTree.getShell(), "Error"
							, "Can only show auxiliary axes when \"" + Scatter2DChartSettingsDialog.CONNECT_POINTS + "\" is turned off.");
				} else {
					layer.getLegend().showAuxilaryAxesDialog(legendTree.getShell());
				}
			});
		}

		if (!layer.isSelectionLayer() && !layer.isAxesLayer()) {
			ToolItem resetLayerButton = new ToolItem(toolBar, SWT.PUSH);
			resetLayerButton.setImage(IconManager.getIconImage("arrow_refresh.png"));
			resetLayerButton.setToolTipText("Reset layer");
			resetLayerButton.addListener(SWT.Selection, e -> {
				if (layer.getLegend().isFilterable()) {
					// Reset Aggregation
					IDataCalculator<ENTITY, ITEM> calculator = layer.getDataProvider().getDataCalculator();
					if (calculator instanceof AggregationDataCalculator) {
						((AggregationDataCalculator<ENTITY, ITEM>) calculator).setAggregationMethod(AggregationDataCalculator.NONE);
						((AggregationDataCalculator<ENTITY, ITEM>) calculator).setAggregationFeature(AggregationDataCalculator.NONE);
					}
					// Reset Filtering
					layer.getDataProvider().setFilters(null);
					layer.getDataProvider().performFiltering();
					getLayerFilterChangedObservable().valueChanged(layer);
				}
				// Reset Data Bounds
				layer.getDataProvider().setDataBounds(null);
				if (layer.getLegend().isShowAuxilaryAxes()) {
					// Reset Auxiliary axes
					layer.getChartSettings().getAuxiliaryChartSettings().clear();
					layer.getDataProvider().getAuxiliaryFeatures().clear();
					layer.dataChanged();
				}
				// Reset Grouping if modifiable
				if (layer.getLegend().canModify(GROUPING)) {
					changeActiveGrouping(0, layer);
				}
				refreshLegendTree();
			});
		}

		if (!layer.isSelectionLayer() && !layer.isAxesLayer()) {
			ToolItem removeLayerButton = new ToolItem(toolBar, SWT.PUSH);
			removeLayerButton.setImage(IconManager.getIconImage("struct_delete.png"));
			removeLayerButton.setToolTipText("Remove layer");
			removeLayerButton.addListener(SWT.Selection, e -> {
				removeChartLayer(layer);
				getLayerRemovedObservable().valueChanged(layer);
				refreshLegendTree();
			});
		}

		final TreeEditor editor = new TreeEditor(legendTree);
		editor.grabHorizontal = true;
		editor.minimumWidth = 250;
		editor.setEditor(toolBar, item, legendTree.getColumnCount() - 1);

		return editor;
	}

	private void createAxesButtons(final ToolBar toolBar, final AbstractChartLayer<ENTITY, ITEM> layer) {
		int dim = layer.getChart().getType().getNumberOfDimensions();
		if (dim > 0) {
			final ToolItem[] featureDropdowns = ChartAxesMenuFactory.initializeAxisButtons(toolBar, dim, true);
			ChartAxesMenuFactory.updateAxisButtons(featureDropdowns, layer.getDataProvider().getFeaturesPerGroup()
					, layer.getDataProvider().getSelectedFeatures()
					, (String axis, int dimension) -> {
						Object[] args = new Object[] { layer, axis, dimension };
						getLayerFeatureChangedObservable().valueChanged(args);
					}
			);
		}
	}

	private void createFilterMenuButtons(final ToolBar toolBar, final AbstractChartLayer<ENTITY, ITEM> layer) {
		List<IFilter<ENTITY, ITEM>> filters = layer.getDataProvider().getFilters();
		if (filters != null) {
			final ToolItem filterMenuButton = new ToolItem(toolBar, SWT.DROP_DOWN);
			filterMenuButton.setImage(IconManager.getIconImage("funnel.png"));
			filterMenuButton.setToolTipText("Apply a filter");

			final Menu filterMenu = new Menu(toolBar.getShell(), SWT.POP_UP);

			for (final IFilter<ENTITY, ITEM> filter : layer.getDataProvider().getFilters()) {
				filter.initialize(filterMenu);
				filter.addValueChangedListener(() -> {
					layer.getDataProvider().performFiltering();
					getLayerFilterChangedObservable().valueChanged(layer);
				});
			}

			filterMenuButton.addListener(SWT.Selection, event -> {
				if (event.detail == SWT.ARROW) {
					Rectangle rect = filterMenuButton.getBounds();
					Point pt = new Point(rect.x, rect.y + rect.height);
					pt = toolBar.toDisplay(pt);
					filterMenu.setLocation(pt.x, pt.y);
					filterMenu.setVisible(true);
				}
			});
			filterMenuButton.addDisposeListener(e -> {
				if (filterMenu != null && !filterMenu.isDisposed()) {
					filterMenu.dispose();
				}
			});
		}
	}

	private List<AbstractLegend<ENTITY, ITEM>> buildLegendModel() {
		List<AbstractLegend<ENTITY, ITEM>> model = new ArrayList<AbstractLegend<ENTITY, ITEM>>();
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			AbstractLegend<ENTITY, ITEM> legend = layer.getLegend();
			if (legend != null) {
				List<? extends AbstractLegendItem<ENTITY, ITEM>> legendItems = legend.createLegendItems();
				if (legendItems != null && legendItems.size() > 1) {
					// Sort by name
					Collections.sort(legendItems, AbstractLegendItem.getComparator());
				}
				legend.setLegendItems(legendItems);
				model.add(legend);
			}
		}

		return model;
	}

	private String[] getGroupingStrategyLabels() {
		String[] groupingLabels = new String[groupingStrategies.size()];
		int i = 0;
		for (IGroupingStrategy<ENTITY, ITEM> strategy : groupingStrategies) {
			groupingLabels[i++] = strategy.getName();
		}
		return groupingLabels;
	}

	private void changeActiveGrouping(int index, AbstractChartLayer<ENTITY, ITEM> layer) {
		layer.getDataProvider().setActiveGroupingStrategy(getGroupingStrategyByPosition(index));

		// When grouping strategy changed -> remove aux settings
		layer.getDataProvider().getAuxiliaryFeatures().clear();
		layer.getChartSettings().getAuxiliaryChartSettings().clear();
		getLayerGroupingChangedObservable().valueChanged(layer);
	}

	private void createRefreshLegendTreeJob() {
		refreshLegendJob = new Job("Refresh Chart Legend") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(() -> {
					if (!legendTree.isDisposed() && !lockedForRefresh) {
						Object[] expandedElements = legendViewer.getExpandedElements();
						TreePath[] expandedPaths = legendViewer.getExpandedTreePaths();

						legendViewer.getTree().setRedraw(false);

						// Cleanup previous model
						cleanupLegendModel(legendViewer.getInput());
						legendViewer.setInput(buildLegendModel());

						legendViewer.refresh();

						legendViewer.setExpandedElements(expandedElements);
						legendViewer.setExpandedTreePaths(expandedPaths);

						createEditors();

						legendViewer.getTree().setRedraw(true);
					}
				});
				return Status.OK_STATUS;
			}
		};
	}

	/*
	 * Protected
	 */

	protected void addSpecificColumns() {
		// do nothing
	}

	protected IGroupingStrategy<ENTITY, ITEM> getGroupingStrategyByPosition(int position) {
		return groupingStrategies.get(position);
	}

	/*
	 * Public
	 */

	public void refreshLegendTree() {
		refreshLegendJob.cancel();
		refreshLegendJob.schedule(500);
	}

	public void updateLayers() {
		// Register for updates on data
		boolean isFirst = true;
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartLayers()) {
			// Selections are made on active layer ( = the ones shown in the legend)
			if (layer.isDataLayer() && !layer.isAxesLayer()) {
				layer.getDataProvider().getDataChangedObservable().deleteObservers();
				if (isFirst) {
					isFirst = false;
					layer.getDataProvider().getDataChangedObservable().addObserver(getDataChangedObserver());
				}
			}
		}
	}

	public String getColumnName(int columnIndex) {
		return COLUMNS[columnIndex];
	}

	public TreeViewer getLegendViewer() {
		return legendViewer;
	}

	public void setGroupingStrategies(List<IGroupingStrategy<ENTITY, ITEM>> groupingStrategies) {
		this.groupingStrategies = groupingStrategies;
	}

	public void setLockedForRefresh(boolean lockedForRefresh) {
		this.lockedForRefresh = lockedForRefresh;
	}

	/*
	 * Observables / Observers
	 */

	public ValueObservable getActiveLayerChangedObservable() {
		return activeLayerChangedObservable;
	}

	public ValueObservable getLayerOrderChangedObservable() {
		return layerOrderChangedObservable;
	}

	public ValueObservable getLayerGroupingChangedObservable() {
		return layerGroupingChangedObservable;
	}

	public ValueObservable getLayerStatusChangedObservable() {
		return layerStatusChangedObservable;
	}

	public ValueObservable getLayerFilterChangedObservable() {
		return layerFilterChangedObservable;
	}

	public ValueObservable getLayerFeatureChangedObservable() {
		return layerFeatureChangedObservable;
	}

	public ValueObservable getLayerRemovedObservable() {
		return layerRemovedObservable;
	}

	public ValueObservable getLayerToggleObservable() {
		return layerToggleObservable;
	}

	public Observer getDataChangedObserver() {
		return (o, arg) -> refreshLegendTree();
	}

	/*
	 * Classes
	 */

	@SuppressWarnings("unchecked")
	private class LegendContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// Do nothing.
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<AbstractLegend<?, ?>>) inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AbstractLegend) {
				AbstractLegend<ENTITY, ITEM> legend = (AbstractLegend<ENTITY, ITEM>) parentElement;
				List<? extends AbstractLegendItem<ENTITY, ITEM>> items = legend.getLegendItems();
				if (items != null) {
					return items.toArray();
				}
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof AbstractLegendItem) {
				return ((AbstractLegendItem<?, ?>) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof AbstractLegend) {
				AbstractLegend<ENTITY, ITEM> legend = (AbstractLegend<ENTITY, ITEM>) element;
				List<? extends AbstractLegendItem<ENTITY, ITEM>> items = legend.getLegendItems();
				return items != null && !items.isEmpty();
			}
			return false;
		}
	}

}