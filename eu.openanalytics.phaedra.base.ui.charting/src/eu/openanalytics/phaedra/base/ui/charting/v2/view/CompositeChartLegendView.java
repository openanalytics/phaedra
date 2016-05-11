package eu.openanalytics.phaedra.base.ui.charting.v2.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SELECTION;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.ui.charting.v2.ChartSelectionManager;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IJEPAwareDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.ChartAxesMenuFactory.AxisChangedListener;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.InteractiveChartView.MouseMode;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyPasteSelectionDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.ui.util.view.ShowSecondaryViewDecorator;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public abstract class CompositeChartLegendView<ENTITY, ITEM> extends DecoratedView {

	private static final String ADD = "Add ";
	private static final String LAYER = " layer";

	private InteractiveChartView<ENTITY, ITEM> chartView;
	private BaseLegendView<ENTITY, ITEM> legendView;

	private SplitComposite splitComposite;
	private Composite chartComposite;

	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IModelEventListener modelEventListener;
	private DropTarget dropTarget;

	private ChartSelectionManager chartSelectionManager;
	private AxisChangedListener axesListener;
	private ToolItem[] featureDropdowns;
	private ToolItem addLayerDropdown;
	private Button focusFixButton;
	private ValueObservable itemSelectionChangedObservable = new ValueObservable();
	private ValueObservable itemHighlightChangedObservable = new ValueObservable();

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		SplitCompositeFactory.getInstance().prepare(memento, SplitComposite.MODE_V_1_2);
	}

	/**
	 * Create the controls for the chart. Optimized solution would be to add
	 * layers directly to the frame but because of flickering issues between
	 * jface/swt/awt/swing we need to add a jrootpane in between. Since we also
	 * need the overlay layout, we need an extra panel as well...
	 */
	@Override
	public void createPartControl(final Composite parent) {

		splitComposite = SplitCompositeFactory.getInstance().create(parent);

		/**
		 * Since nested chart elements all sit in a rootpane (awt -> swing), and
		 * handle mouse events themselves, the view doesn't get the required
		 * focus (resulting in events not being propagated)
		 *
		 * Therefore we explicitly add a focus button as an extra element in the
		 * composite. And set the focus to this (invisible) button. (A composite
		 * needs at least one element to be able to get focus)
		 */
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(parent);
		focusFixButton = new Button(parent, SWT.PUSH);
		focusFixButton.setText("fix focus");
		// TODO: Try Drag & Drop instead of context menu action.
		/**
		 * Mouse events, including Drag Events are caught by the AWT part.
		 * These events are not passed to the SWT Parent.
		 *
		 * See chartView.getActivatedObservable()
		 * If we can manage to manually send the Mouse Event based on the AWT Mouse Event it might work.
		 *
		 * SWT to AWT Example
		 * http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet319.java
		 */
		//		focusFixButton.addKeyListener(new KeyListener() {
		//			@Override
		//			public void keyReleased(KeyEvent e) {
		//				if (e.keyCode == SWT.SHIFT) {
		//					if (chartComposite.getData(DND.DRAG_SOURCE_KEY) == null) {
		//						chartView.removeSelectionLayer();
		//						final DragSource dragSource = new DragSource(chartComposite, DND.DROP_COPY);
		//						dragSource.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer() });
		//						dragSource.addDragListener(new DragSourceAdapter() {
		//							@Override
		//							public void dragStart(DragSourceEvent event) {
		//								System.out.println("Drag Start");
		//							}
		//							@Override
		//							public void dragFinished(DragSourceEvent event) {
		//								System.out.println("Drag Finish");
		//							}
		//							@Override
		//							public void dragSetData(DragSourceEvent event) {
		//								System.out.println("Drag Set");
		//							}
		//						});
		//					}
		//				}
		//			}
		//			@Override
		//			public void keyPressed(KeyEvent e) {
		//				if (e.keyCode == SWT.SHIFT) {
		//				}
		//			}
		//		});
		GridDataFactory.fillDefaults().hint(0, 0).applyTo(focusFixButton);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComposite);

		chartComposite = new Composite(splitComposite, SWT.EMBEDDED | SWT.NO_BACKGROUND | SWT.BORDER);

		List<AbstractChartLayer<ENTITY, ITEM>> layers = createChartLayers();

		chartView = new InteractiveChartView<ENTITY, ITEM>(chartComposite, layers);
		legendView = createLegendView(splitComposite, layers);

		legendView.getActiveLayerChangedObservable().addObserver(chartView.getActiveLayerChangedObserver());
		legendView.getLayerOrderChangedObservable().addObserver(chartView.getLayerOrderChangedObserver());
		legendView.getLayerGroupingChangedObservable().addObserver(chartView.getLayerGroupingChangedObserver());
		legendView.getLayerStatusChangedObservable().addObserver(chartView.getLayerStatusChangedObserver());
		legendView.getLayerFilterChangedObservable().addObserver(chartView.getLayerFilterChangedObserver());
		legendView.getLayerFeatureChangedObservable().addObserver(getLayerFeatureChangedObserver());
		legendView.getLayerRemovedObservable().addObserver(chartView.getLayerRemovedObserver());
		legendView.getLayerToggleObservable().addObserver(chartView.getLayerToggleObserver());

		// Handle the activation/focus stuff of the chart
		chartView.getActivatedObservable().addObserver((o, arg) -> {
			setFocus();
			// dirty hack because of swt/awt mouse events not coming through
			if (arg != null && arg instanceof MouseEvent) {
				if (((MouseEvent) arg).getButton() == MouseEvent.BUTTON3 && getContextMenu() != null) {
					getContextMenu().getMenu().setVisible(true);
				}
			}
		});

		chartView.getDataChangedObservable().addObserver(legendView.getDataChangedObserver());
		chartView.getDataChangedObservable().addObserver((Observable o, Object arg) -> {
			if (chartView != null) reloadFeatures(chartView.getFeaturesPerGroup());
		});
		chartView.getSelectionChangedObservable().addObserver((o, arg) -> {
			Display.getDefault().asyncExec(() -> {
				if (arg instanceof ISelection) {
					chartSelectionManager.setSelection((ISelection) arg);
					chartSelectionManager.fireSelection();
				}
			});
		});

		getItemSelectionChangedObservable().addObserver(chartView.getEntitySelectionChangedObserver());
		getItemSelectionChangedObservable().addObserver((o, arg) -> {
			if (arg instanceof List && chartSelectionManager != null) {
				ISelection sel = new StructuredSelection((List<?>) arg);
				chartSelectionManager.setSelection(sel);
				chartSelectionManager.fireSelection();
			}
		});

		getItemHighlightChangedObservable().addObserver(chartView.getEntityHighlightChangedObserver());
		getItemHighlightChangedObservable().addObserver((o, arg) -> {
			if (arg instanceof List && chartSelectionManager != null) {
				ISelection sel = new StructuredSelection((List<?>) arg);
				chartSelectionManager.setSelection(sel);
				chartSelectionManager.fireSelection();
			}
		});

		splitComposite.setWeights(new int[] { 80, 20 });

		this.selectionListener = initializeSelectionListener();
		if (selectionListener != null) {
			getSite().getPage().addSelectionListener(selectionListener);
			SelectionUtils.triggerActiveSelection(selectionListener);
		}
		this.highlightListener = initializeHighlightListener();
		if (highlightListener != null) {
			getSite().getPage().addSelectionListener(highlightListener);
			SelectionUtils.triggerActiveSelection(highlightListener);
		}

		this.modelEventListener = initializeModelEventListener();
		if (this.modelEventListener != null) {
			ModelEventService.getInstance().addEventListener(modelEventListener);
		}

		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener, true));
		addDecorator(new CopyableDecorator());
		addDecorator(new ShowSecondaryViewDecorator());
		addDecorator(new CopyPasteSelectionDecorator(CopyPasteSelectionDecorator.COPY));

		// Init all decorators (e.g. for rendering menu etc)
		initDecorators(parent, chartComposite);

		// Init drop Target
		dropTarget = createDropTarget(chartComposite);

		// register selections ...
		chartSelectionManager = new ChartSelectionManager();
		getSite().setSelectionProvider(chartSelectionManager);

		// catch mouse wheel events + pass to chart view (since swt/awt blocks this...)
		parent.addMouseWheelListener(getChartView());

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui." + getClass().getSimpleName());
	}

	public BaseLegendView<ENTITY, ITEM> createLegendView(Composite composite, List<AbstractChartLayer<ENTITY, ITEM>> layers) {
		return new BaseLegendView<ENTITY, ITEM>(composite, layers, getGroupingStrategies());
	}

	public abstract List<AbstractChartLayer<ENTITY, ITEM>> createChartLayers();

	public AbstractChartLayer<ENTITY, ITEM> createLayer(ChartName name) {
		AbstractChartLayer<ENTITY, ITEM> layer = getChartLayerFactory().createLayer(name);

		if (layer != null && layer.isDataLayer() && getChartView() != null) {
			// Set selected features
			layer.getDataProvider().setSelectedFeatures(getChartView().getSelectedFeatures());
			// If the data provider is a JEP enabled provider, copy the expressions as well.
			if (layer.getDataProvider() instanceof IJEPAwareDataProvider) {
				IJEPAwareDataProvider provider = (IJEPAwareDataProvider) layer.getDataProvider();
				provider.setJepExpressions(getChartView().getJEPExpressions());
			}
		}
		return layer;
	}

	public AbstractChartLayer<ENTITY, ITEM> createLayer(LayerSettings<ENTITY, ITEM> settings, List<ITEM> entities) {
		return getChartLayerFactory().createLayer(settings, entities);
	}

	public abstract List<IGroupingStrategy<ENTITY, ITEM>> getGroupingStrategies();

	@Override
	protected void fillToolbar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				createToolbarButtons(parent);
			}
			@Override
			public boolean isDynamic() {
				// Prevent issues with toggling user mode in Phaedra.
				return true;
			}
		});

		// Split mode button.
		tbm.add(splitComposite.createModeButton());

		super.fillToolbar();
	}

	protected DropTarget createDropTarget(Control parent) {
		// No drop support.
		return null;
	}

	protected void addSpecificToolbarButtons(ToolBar parent) {
		// Do nothing.
	}

	@SuppressWarnings("unchecked")
	private Observer getLayerFeatureChangedObserver() {
		return (o, arg) -> {
			Object[] args = (Object[]) arg;
			axisFeatureChanged((AbstractChartLayer<ENTITY, ITEM>) args[0], (String) args[1], (int) args[2]);
		};
	}

	private void createToolbarButtons(ToolBar parent) {
		// add layer buttons
		addLayerDropdown = DropdownToolItemFactory.createDropdown(parent);
		addLayerDropdown.setImage(IconManager.getIconImage("struct_add.png"));
		addLayerDropdown.setToolTipText("Add layer");

		reloadAddableLayers(chartView.getChartLayers());

		// selected points only
		final ToolItem showShowSelectedOnlyButton = new ToolItem(parent, SWT.CHECK);
		showShowSelectedOnlyButton.setImage(IconManager.getIconImage("wand.png"));
		showShowSelectedOnlyButton.setToolTipText("Show highlighted points only");
		showShowSelectedOnlyButton.setSelection(chartView.isShowSelectedOnly());
		showShowSelectedOnlyButton.addListener(SWT.Selection, e -> {
			chartView.setShowSelectedOnly(!chartView.isShowSelectedOnly());
			showShowSelectedOnlyButton.setSelection(chartView.isShowSelectedOnly());
		});

		// zoom/lasso/select buttons
		createZoomMoveSelectButtons(parent);

		// rescale button
		final ToolItem rescaleButton = new ToolItem(parent, SWT.PUSH);
		rescaleButton.setImage(IconManager.getIconImage("arrow_out.png"));
		rescaleButton.setToolTipText("Rescale the plot to show all points");
		rescaleButton.addListener(SWT.Selection, e -> chartView.resetZoom());

		// Axes feature selection buttons
		int dimensions = chartView.getNumberOfDimensionsToBeShown();
		// Dynamic dimension plots need 1 checkbox button.
		final boolean isMultiFeature = dimensions == -1;
		if (isMultiFeature) dimensions = 1;

		axesListener = (String axis, int dimension) -> {
			if (isMultiFeature) axisFeatureChanged(axis);
			else axisFeatureChanged(axis, dimension);
			// Update the Selected Features labels in Legend.
			legendView.refreshLegendTree();
		};

		featureDropdowns = ChartAxesMenuFactory.initializeAxisButtons(parent, dimensions, !isMultiFeature);
		reloadFeatures(chartView.getFeaturesPerGroup());

		addSpecificToolbarButtons(parent);
	}

	private void reloadFeatures(Map<String, List<String>> groupedFeatures) {
		ChartAxesMenuFactory.updateAxisButtons(featureDropdowns, groupedFeatures, chartView.getSelectedFeatures(), axesListener);
	}

	private void reloadAddableLayers(List<AbstractChartLayer<ENTITY, ITEM>> layers) {
		DropdownToolItemFactory.clearChildren(addLayerDropdown);

		if (layers == null) return;
		for (ChartName possibleLayer : getPossibleLayers()) {
			MenuItem layerItem = DropdownToolItemFactory.createChild(addLayerDropdown,
					ADD + possibleLayer.getDecription() + LAYER, SWT.NONE);

			layerItem.addListener(SWT.Selection, e -> {
				MenuItem selected = (MenuItem) e.widget;
				String text = selected.getText();
				text = text.substring(ADD.length(), text.length() - LAYER.length());
				getChartView().setVisible(false);
				try {
					AbstractChartLayer<ENTITY, ITEM> layer = createLayer(ChartName.getByDescription(text));
					if (layer == null) return;

					// Set chart layers
					boolean hadSelectionLayer = getChartView().removeSelectionLayer();
					getChartView().addChartLayer(layer);
					getChartView().initializeLayer(layer);

					if (hadSelectionLayer) {
						// Selection layer
						AbstractChartLayer<ENTITY, ITEM> selectionLayer = createLayer(SELECTION);
						getChartView().addChartLayer(selectionLayer);
						getChartView().initializeLayer(selectionLayer);
					}

					// Reload data
					List<ITEM> entities = getChartView().getBottomEnabledLayer().getDataProvider()
							.getCurrentItems();
					getChartView().reloadDataForAllLayers(entities);

					// Legend layers
					getLegendView().setChartLayers(getChartView().getChartLayers());
					getLegendView().updateLayers();
					legendView.getLayerOrderChangedObservable().valueChanged();
				} finally {
					getChartView().setVisible(true);
				}
			});
		}
	}

	public void axisFeatureChanged(String feature, int dimension) {
		// If the new Feature is the same as the current Feature, do not update chart.
		List<AbstractChartLayer<ENTITY, ITEM>> layers = new ArrayList<>();
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartView().getChartLayers()) {
			if (!layer.isDataLayer()) continue;

			String currFeature = layer.getDataProvider().getSelectedFeature(dimension);
			if (currFeature.equalsIgnoreCase(feature)) continue;

			layers.add(layer);
		}
		if (layers.isEmpty()) return;

		JobUtils.runUserJob(monitor -> {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 95);
			for (AbstractChartLayer<ENTITY, ITEM> layer : layers) {
				if (monitor.isCanceled()) return;
				layer.getDataProvider().setSelectedFeature(feature, dimension, subMonitor);
				subMonitor = new NullProgressMonitor();
			}

			// Recalculate bounds for all layers
			chartView.recalculateDataBounds(new SubProgressMonitor(monitor, 5));
		}, getPartName() + ": Updating axis", 100, toString(), null);
	}

	public void axisFeatureChanged(AbstractChartLayer<ENTITY, ITEM> layer, String feature, int dimension) {
		// If the new Feature is the same as the current Feature, do not update chart.
		String currFeature = layer.getDataProvider().getSelectedFeature(dimension);
		if (currFeature.equalsIgnoreCase(feature)) return;

		JobUtils.runUserJob(monitor -> {
			layer.getDataProvider().setSelectedFeature(feature, dimension, new SubProgressMonitor(monitor, 95));

			if (monitor.isCanceled()) return;

			// Recalculate bounds for all layers
			chartView.recalculateDataBounds(new SubProgressMonitor(monitor, 5));
		}, getPartName() + ": Updating axis", 100, toString(), null);
	}

	public void axisFeatureChanged(String feature) {
		JobUtils.runUserJob(monitor -> {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 95);
			for (AbstractChartLayer<ENTITY, ITEM> layer : getChartView().getChartLayers()) {
				if (monitor.isCanceled()) return;
				if (layer.isDataLayer()) {
					List<String> selectedFeatures = layer.getDataProvider().getSelectedFeatures();
					layer.getDataProvider().setSelectedFeatures(selectedFeatures, subMonitor);
					subMonitor = new NullProgressMonitor();
				}
			}

			// Recalculate bounds for all layers
			chartView.recalculateDataBounds(new SubProgressMonitor(monitor, 5));
		}, getPartName() + ": Updating axis", 100, toString(), null);
	}

	protected void createZoomMoveSelectButtons(ToolBar parent) {
		final ToolItem mouseModeButton = new ToolItem(parent, SWT.PUSH);
		MouseMode mouseMode = chartView.getMouseMode();
		mouseModeButton.setImage(mouseMode.getImage());
		mouseModeButton.setToolTipText(mouseMode.getTooltip());

		mouseModeButton.addListener(SWT.Selection, e -> {
			chartView.toggleMouseMode();

			mouseModeButton.setImage(chartView.getMouseMode().getImage());
			mouseModeButton.setToolTipText(chartView.getMouseMode().getTooltip());
		});

		chartView.initializeMouseMode();
	}

	public abstract ISelectionListener initializeSelectionListener();

	public abstract ISelectionListener initializeHighlightListener();

	public abstract IModelEventListener initializeModelEventListener();

	@Override
	public void setFocus() {
		if (!focusFixButton.isDisposed()) {
			focusFixButton.setFocus();
		}
	}

	public InteractiveChartView<ENTITY, ITEM> getChartView() {
		return chartView;
	}

	protected ValueObservable getItemSelectionChangedObservable() {
		return itemSelectionChangedObservable;
	}

	protected ValueObservable getItemHighlightChangedObservable() {
		return itemHighlightChangedObservable ;
	}

	public abstract ChartLayerFactory<ENTITY, ITEM> getChartLayerFactory();

	@Override
	public void dispose() {
		JobUtils.cancelJobs(toString());

		if (selectionListener != null) getSite().getPage().removeSelectionListener(selectionListener);
		if (highlightListener != null) getSite().getPage().removeSelectionListener(highlightListener);
		if (modelEventListener != null) ModelEventService.getInstance().removeEventListener(modelEventListener);

		if (dropTarget != null) dropTarget.dispose();

		itemSelectionChangedObservable.deleteObservers();
		itemSelectionChangedObservable = null;
		itemHighlightChangedObservable.deleteObservers();
		itemHighlightChangedObservable = null;

		chartView.dispose();
		chartView = null;

		chartComposite.dispose();

		splitComposite.dispose();
		focusFixButton.dispose();
		super.dispose();
	}

	public abstract List<ChartName> getPossibleLayers();

	public BaseLegendView<ENTITY, ITEM> getLegendView() {
		return legendView;
	}

	public boolean isSVG() {
		for (AbstractChartLayer<ENTITY, ITEM> layer : getChartView().getChartLayers()) {
			if (layer.getChart() == null || !layer.getChart().isSupportSVG()) {
				return false;
			}
		}
		return true;
	}

}