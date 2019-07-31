package eu.openanalytics.phaedra.ui.curve.details;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.misc.HyperlinkLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.split.SplitComposite;
import eu.openanalytics.phaedra.base.ui.util.split.SplitCompositeFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitErrorCode;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider;
import eu.openanalytics.phaedra.model.curve.util.CurveTextProvider.CurveTextField;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.curve.Activator;
import eu.openanalytics.phaedra.ui.curve.cmd.CmdUtil;
import eu.openanalytics.phaedra.ui.curve.cmd.EditCurve;
import eu.openanalytics.phaedra.ui.curve.cmd.ResetCurve;
import eu.openanalytics.phaedra.ui.curve.prefs.PreferencePage;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;


public class CrcDetailsView extends DecoratedView {

	private BreadcrumbViewer breadcrumb;
	private SplitComposite splitComp;
	private MultiSelectChart chartComposite;
	private RichTableViewer infoTable;
	private DefaultToolTip tooltip;

	private ISelectionListener selectionListener;
	private IModelEventListener curveFitListener;
	private IUIEventListener featureListener;
	private IPropertyChangeListener prefListener;
	private WellSelectionProvider selectionProvider = new WellSelectionProvider();
	
	private Curve currentCurve;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		SplitCompositeFactory.getInstance().prepare(memento, SplitComposite.MODE_V_1_2);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		splitComp.save(memento);
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(parent);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		splitComp = SplitCompositeFactory.getInstance().create(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(splitComp);

		// A JFreeChart container, with custom pop-up menu and click listener.
		chartComposite = new MultiSelectChart(splitComp, SWT.BORDER, createChart()) {
			@Override
			public void handleSelection(List<XYItemEntity> selectedItems) {
				// Default selection behavior.
				super.handleSelection(selectedItems);
				// Create & trigger a Well selection.
				List<XYItemEntity> sel = getCurrentSelection();
				List<Well> wellSelection = new ArrayList<Well>();
				if (sel != null) {
					for (XYItemEntity item : sel) {
						XYSeriesCollection ds = (XYSeriesCollection) item.getDataset();
						XYSeries series = ds.getSeries(item.getSeriesIndex());
						XYDataItem dataItem = series.getDataItem(item.getItem());
						if (dataItem instanceof CrcChartItem) {
							wellSelection.add(((CrcChartItem) dataItem).getWell());
						}
					}
				}
				selectionProvider.fireSelectionEvent(wellSelection);
			}

			@Override
			protected void displayPopupMenu(int x, int y) {
				Menu menu = chartComposite.getMenu();
				menu.setLocation(x, y);
				menu.setVisible(true);
			}
		};
		GridDataFactory.fillDefaults().grab(true, true).applyTo(chartComposite);

		infoTable = new RichTableViewer(splitComp, SWT.BORDER);
		infoTable.applyColumnConfig(createTableColumns());
		infoTable.setContentProvider(new ArrayContentProvider());
		infoTable.setInput(CurveTextProvider.getColumns(null));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(infoTable.getTable());

		tooltip = new DefaultToolTip(infoTable.getControl(), SWT.NONE, true);

		// Listen to incoming Curve selections.
		selectionListener = (part, selection) -> {
			if (part == CrcDetailsView.this) return;
			boolean updateRequired = false;

			Feature feature = ProtocolUIService.getInstance().getCurrentFeature();
			Curve curve = SelectionUtils.getFirstObject(selection, Curve.class);
			if (curve == null) {
				Well well = SelectionUtils.getFirstObject(selection, Well.class);
				if (well != null) curve = CurveFitService.getInstance().getCurve(well, feature);
			}
			if (curve == null) {
				Compound compound = SelectionUtils.getFirstObject(selection, Compound.class);
				if (compound != null) curve = CurveFitService.getInstance().getCurve(compound.getWells().get(0), feature);
			}
				
			if (curve != null && !curve.equals(currentCurve)) {
				currentCurve = curve;
				updateRequired = true;
			}
			
			// If a new feature or compound were selected, update the chart.
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			if (updateRequired) {
				selectionProvider.setSelection(selection);
				refreshBreadcrumb();
				update();
			} else {
				long sampleCount = wells.stream().filter(w -> w.getCompound() != null).count();
				if (!wells.isEmpty() && sampleCount == 0) {
					// Only controls/empties selected: clear chart.
					currentCurve = null;
					refreshBreadcrumb();
					update();
				}
			}

			// See if there are Wells in the selection.
			if (wells != null && chartComposite.getChart() != null) highlightWells(wells);
		};
		if (getSite() != null) getSite().getPage().addSelectionListener(selectionListener);

		// Listen to Curve fit events: refresh the chart if that happens.
		curveFitListener = (event) -> {
			if (event.source instanceof Curve && (event.type == ModelEventType.CurveFit || event.type == ModelEventType.CurveFitFailed)) {
				if (event.source.equals(currentCurve)) Display.getDefault().asyncExec(() -> update());	
			}
		};
		ModelEventService.getInstance().addEventListener(curveFitListener);

		featureListener = (event) -> {
			if (event.type == EventType.FeatureSelectionChanged) {
				if (currentCurve == null) return;
				Feature feature = ProtocolUIService.getInstance().getCurrentFeature();
				Well well = currentCurve.getCompounds().get(0).getWells().get(0);
				currentCurve = CurveFitService.getInstance().getCurve(well, feature);
				update();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureListener);

		prefListener = (event) -> {
			if (event.getProperty().startsWith("CRC_")) update();
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		
		splitComp.setWeights(new int[] { 50, 50 });
		if (getSite() != null) createToolBar(parent);
		if (getSite() != null) getSite().setSelectionProvider(selectionProvider);

		addDecorator(new CopyableDecorator());
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		initDecorators(parent, chartComposite);

		// Try to get an initial selection from the page.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewDoseResponseCurve");
	}

	@Override
	public void setFocus() {
		infoTable.getTable().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (selectionListener != null) getSite().getPage().removeSelectionListener(selectionListener);
		if (curveFitListener != null) ModelEventService.getInstance().removeEventListener(curveFitListener);
		if (featureListener != null) ProtocolUIService.getInstance().removeUIEventListener(featureListener);
		if (prefListener != null) Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		Action action = new Action("Edit Curve", SWT.PUSH) {
			@Override
			public void run() {
				CmdUtil.executeCmd(EditCurve.class.getName(), currentCurve);
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("pencil.png"));
		manager.add(action);
		
		action = new Action("Reset Curve", SWT.PUSH) {
			@Override
			public void run() {
				CmdUtil.executeCmd(ResetCurve.class.getName(), currentCurve);
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("pencil_delete.png"));
		manager.add(action);
		
		manager.add(new Separator());
		
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		super.fillContextMenu(manager);
	}

	/*
	 * Non-public helpers
	 * ******************
	 */

	/**
	 * Make sure the view displays the most recent information about curve &
	 * compound. Should be called after every selection change.
	 */
	private void update() {
		// SplitComposite uses visibility to hide components. Prevent setInput() from resetting visibility.
		infoTable.setInput(CurveTextProvider.getColumns(currentCurve));
		infoTable.getTable().setVisible(splitComp.isVisible(2));

		// Refresh the chart.
		chartComposite.setChart(createChart());
		chartComposite.forceRedraw();
	}

	private void highlightWells(List<Well> wells) {
		if (chartComposite == null) return;

		List<XYItemEntity> selectedItems = new ArrayList<XYItemEntity>();
		EntityCollection col = chartComposite.getChartRenderingInfo().getEntityCollection();
		for (Object entity : col.getEntities()) {
			if (entity instanceof XYItemEntity) {
				XYItemEntity item = (XYItemEntity)entity;
				XYSeriesCollection ds = (XYSeriesCollection) item.getDataset();
				XYSeries series = ds.getSeries(item.getSeriesIndex());
				XYDataItem dataItem = series.getDataItem(item.getItem());
				if (dataItem instanceof CrcChartItem) {
					CrcChartItem cItem = (CrcChartItem) dataItem;
					Well well = cItem.getWell();
					if (wells.contains(well)) selectedItems.add(item);
				}
			}
		}
		chartComposite.handleSelection(selectedItems);
	}

	private void refreshBreadcrumb() {
		Object newInput = currentCurve == null ? null : currentCurve.getCompounds().get(0);
		breadcrumb.setInput(newInput);
		breadcrumb.getControl().getParent().layout();
	}
	
	private JFreeChart createChart() {
		return CrcChartFactory.createChart(currentCurve);
	}

	private ColumnConfiguration[] createTableColumns() {
		ColumnConfiguration[] config = new ColumnConfiguration[2];
		config[0] = ColumnConfigFactory.create("Property", ColumnDataType.String, 120);
		config[0].setLabelProvider(new RichLabelProvider(config[0]){
			@Override
			public String getText(Object element) {
				return ((CurveTextField) element).getLabel();
			}
		});
		config[1] = ColumnConfigFactory.create("Value", ColumnDataType.String, 250);
		config[1].setLabelProvider(new HyperlinkLabelProvider(infoTable.getControl(), 1) {
			@Override
			public String getText(Object element) {
				if (currentCurve == null) return "";
				return ((CurveTextField) element).renderValue(currentCurve);
			}
			@Override
			protected void handleLinkClick(Object element) {
				if (currentCurve == null) return;
				String errDesc = CurveFitErrorCode.getDescription(currentCurve);
				tooltip.setText(errDesc == null ? "No description" : errDesc);
				tooltip.show(new Point(0, 0));
			}
			@Override
			protected boolean isHyperlinkEnabled(Object element) {
				String label = ((CurveTextField) element).getLabel();
				return label.equals("Fit Error");
			}
		});
		return config;
	}

	private void createToolBar(Composite parent) {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		ContributionItem item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem ti = new ToolItem(parent, SWT.PUSH);
				ti.setImage(IconManager.getIconImage("pencil.png"));
				ti.setToolTipText("Edit Curve");
				ti.addListener(SWT.Selection, (event) -> {
					if (currentCurve != null) CmdUtil.executeCmd(EditCurve.class.getName(), currentCurve);
				});
			}
		};
		mgr.add(item);

		item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem ti = new ToolItem(parent, SWT.PUSH);
				ti.setImage(IconManager.getIconImage("pencil_delete.png"));
				ti.setToolTipText("Reset Curve");
				ti.addListener(SWT.Selection, (event) -> {
					if (currentCurve != null) CmdUtil.executeCmd(ResetCurve.class.getName(), currentCurve);
				});
			}
		};
		mgr.add(item);

		item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem ti = new ToolItem(parent, SWT.PUSH);
				ti.setImage(IconManager.getIconImage("settings.gif"));
				ti.setToolTipText("Preferences");
				ti.addListener(SWT.Selection, (event) -> {
					PreferencesUtil.createPreferenceDialogOn(Display.getCurrent().getActiveShell(), PreferencePage.class.getName(), null, null).open();
				});
			}
		};
		mgr.add(item);
		
		mgr.add(splitComp.createModeButton());
	}

	private static class WellSelectionProvider implements ISelectionProvider {
		private ListenerList<ISelectionChangedListener> listenerMgr;
		private List<Well> currentSelection;

		public WellSelectionProvider() {
			this.listenerMgr = new ListenerList<>();
		}

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listenerMgr.add(listener);
		}

		@Override
		public ISelection getSelection() {
			if (currentSelection == null)
				return StructuredSelection.EMPTY;
			return new StructuredSelection(currentSelection);
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			listenerMgr.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			if (selection instanceof StructuredSelection) {
				StructuredSelection sel = (StructuredSelection) selection;
				List<Well> newSelection = new ArrayList<Well>();
				Iterator<?> it = sel.iterator();
				while (it.hasNext()) {
					Object o = it.next();
					if (o instanceof Well) {
						newSelection.add((Well) o);
					}
				}
				currentSelection = newSelection;
			}
		}

		public void fireSelectionEvent(List<Well> wells) {
			currentSelection = wells;
			ISelection selection = getSelection();
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			final Object[] listeners = listenerMgr.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				ISelectionChangedListener listener = (ISelectionChangedListener)listeners[i];
				listener.selectionChanged(event);
			}
		}
	}

}
