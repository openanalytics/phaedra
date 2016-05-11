package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.filter.FilterItemsSelectionDialog;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.BaseFeatureInput;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeatureContentProvider;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.CorrelationProvider.CorrelationType;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.dialog.FeatureSelectionDialog;

public abstract class BaseCorrelationMatrix<FEATURE extends IFeature> extends DecoratedView {

	private static final String ACCEPTED = "Accepted";
	private static final String REJECTED = "Rejected";
	private static final String[] SORT_ORDER = { "[ASC]", "[DESC]" };

	private GridViewer gridViewer;
	private GridLayerSupport layerSupport;

	private ProtocolClass currentPc;
	private List<Well> currentWells;
	private List<FEATURE> features;
	private CorrelationType sortType;
	private int sortOrder;

	private List<String> wellTypeFilter;
	private List<String> wellStatusFilter;

	private ISelectionListener selectionListener;

	private String gridId;
	private Class<FEATURE> featureClass;

	public BaseCorrelationMatrix(String gridId, Class<FEATURE> featureClass) {
		this.gridId = gridId;
		this.featureClass = featureClass;
	}

	@Override
	public void createPartControl(Composite parent) {
		this.features = new ArrayList<>();
		this.wellTypeFilter = getAllWellTypes();
		this.wellStatusFilter = getAllWellStatus();

		gridViewer = new GridViewer(parent);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());

		layerSupport = new GridLayerSupport(gridId, gridViewer);
		layerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
		layerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getViewSite().getActionBars().getServiceLocator() == null);

		gridViewer.setContentProvider(new FeatureContentProvider<FEATURE>());
		gridViewer.setLabelProvider(layerSupport.createLabelProvider());
		gridViewer.getGrid().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT) {
					toggleDragSupport(false);
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.SHIFT && !gridViewer.getGrid().isDragging()) {
					toggleDragSupport(true);
				}
			}
		});

		selectionListener = (part, selection) -> {
			if (part == BaseCorrelationMatrix.this) return;

			ProtocolClass pc = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
			List<Well> wells = getWells(selection);
			if (wells != null && !wells.isEmpty() && !CollectionUtils.equalsIgnoreOrder(wells, currentWells)) {
				// Check if ProtocolClass has changed
				if (pc != null && (currentPc == null || !(currentPc.equals(pc)))) {
					currentPc = pc;
					features.clear();
					List<FEATURE> allFeatures = getFeatures(currentPc);
					for (FEATURE f : allFeatures) {
						if (f.isNumeric() && f.isKey()) features.add(f);
					}
					if (features.isEmpty()) {
						// No key Features available, add first 20 numeric Features.
						int i = 0;
						for (FEATURE f : allFeatures) {
							if (f.isNumeric()) features.add(f);
							if (i++ == 19) break;
						}
					}
				}
				// Do well selection logic
				currentWells = wells;
				setInput();
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent, gridViewer.getControl());

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewWellFeatureCorrelationMatrix");
	}

	@Override
	public void setFocus() {
		gridViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	@Override
	protected void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		ContributionItem item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {

				ToolItem item = DropdownToolItemFactory.createDropdown(parent);
				item.setImage(IconManager.getIconImage("arrows.png"));
				item.setToolTipText("Sorting");
				for (CorrelationType type : CorrelationType.values()) {
					for (int i = 0; i < SORT_ORDER.length; i++) {
						int order = (i == 1 ? SWT.UP : SWT.DOWN);
						Image icon = IconManager.getIconImage(i == 1 ? "arrow_up.png" : "arrow_down.png");
						String sortingText = type.getName() + " " + SORT_ORDER[i];
						MenuItem menuItem = DropdownToolItemFactory.createChild(item, sortingText, SWT.RADIO);
						menuItem.setImage(icon);
						menuItem.addListener(SWT.Selection, e -> {
							if (menuItem.getSelection()) {
								sortType = type;
								sortOrder = order;
								setInput();
							}
						});
						menuItem.setSelection(sortType == type);
					}
				}

				Menu filterMenu = createFilterMenu(parent);

				item = new ToolItem(parent, SWT.NONE);
				item.setImage(IconManager.getIconImage("funnel.png"));
				item.setToolTipText("Filtering");
				item.addListener(SWT.Selection, e -> {
					ToolItem button = (ToolItem) e.widget;
					Rectangle bounds = button.getBounds();
					Point pt = parent.toDisplay(bounds.x, bounds.y + bounds.height);
					filterMenu.setLocation(pt);
					filterMenu.setVisible(true);
				});
				item.addDisposeListener(e -> {
					if (filterMenu != null && !filterMenu.isDisposed()) filterMenu.dispose();
				});

				item = new ToolItem(parent, SWT.NONE);
				item.setImage(IconManager.getIconImage("chart_x.png"));
				item.setToolTipText("Add/Remove Features");
				item.addListener(SWT.Selection, e -> {
					if (currentPc == null) return;
					new FeatureSelectionDialog<FEATURE>(Display.getDefault().getActiveShell()
							, currentPc, featureClass, features, 3).open();

					setInput();
				});
			}
		};
		mgr.add(item);

		super.fillToolbar();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		layerSupport.contributeContextMenu(manager);
		manager.add(new Separator());
		super.fillContextMenu(manager);
	}

	protected abstract List<Well> getWells(ISelection selection);

	protected abstract List<FEATURE> getFeatures(ProtocolClass pClass);

	protected abstract FEATURE getFeatureByID(Long featureId);

	protected abstract BaseFeatureInput<FEATURE> createBaseFeatureInput(List<FEATURE> features, List<Well> wells
			, CorrelationType sortType, int order);

	private List<String> getAllWellStatus() {
		return new ArrayList<>(Arrays.asList(ACCEPTED, REJECTED));
	}

	private List<String> getAllWellTypes() {
		return CollectionUtils.transform(ProtocolService.getInstance().getWellTypes(), ProtocolUtils.WELLTYPE_CODES);
	}

	private void setInput() {
		boolean hasTypeFilter = wellTypeFilter.size() != getAllWellTypes().size();
		boolean hasStatusFilter = wellStatusFilter.size() != getAllWellStatus().size();
		List<Well> wells = new ArrayList<>();
		currentWells.forEach(w -> {
			if (hasStatusFilter && !wellStatusFilter.contains(getWellStatus(w))) return;
			if (hasTypeFilter && !wellTypeFilter.contains(w.getWellType())) return;
			wells.add(w);
		});
		BaseFeatureInput<FEATURE> input = createBaseFeatureInput(features, wells, sortType, sortOrder);
		layerSupport.setInput(input);
	}

	private void toggleDragSupport(boolean enabled) {
		gridViewer.getGrid().setSelectionEnabled(!enabled);
		disposeExistingDragSupport();
		if (enabled) {
			gridViewer.addDragSupport(DND.DROP_LINK, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new DragSourceAdapter() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void dragStart(DragSourceEvent event) {
					// Retrieve current Feature selection of GridViewer.
					Set<FEATURE> featureSet = new HashSet<>();
					// Each GridCell contains an ArrayList of 2 Features. Hence the List.class instead of Feature.class.
					List<List> lists = SelectionUtils.getObjects(gridViewer.getSelection(), List.class);
					for (List<?> l : lists) {
						List<FEATURE> features = (List<FEATURE>) l;
						featureSet.addAll(features);
					}
					LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(new ArrayList(featureSet)));
				}
				@Override
				public void dragFinished(DragSourceEvent event) {
					gridViewer.getGrid().setSelectionEnabled(true);
					disposeExistingDragSupport();
				}
			});
		}
	}

	private void disposeExistingDragSupport() {
		Object o = gridViewer.getGrid().getData(DND.DRAG_SOURCE_KEY);
		if (o != null) {
			((DragSource) o).dispose();;
		}
	}

	private String getWellStatus(Well well) {
		return well.getStatus() >= 0 ? ACCEPTED : REJECTED;
	}

	private Menu createFilterMenu(ToolBar parent) {
		Menu filterMenu = new Menu(parent.getShell(), SWT.POP_UP);

		// Add Well Type Filter.
		MenuItem menuItem = new MenuItem(filterMenu, SWT.PUSH);
		menuItem.setText("Well Type Filter");
		menuItem.addListener(SWT.Selection, e -> {
			FilterItemsSelectionDialog dialog = new FilterItemsSelectionDialog("Well Type Filter", "Uncheck Well Types to hide them.",
					getAllWellTypes(), wellTypeFilter);
			if (dialog.open() == IDialogConstants.OK_ID) {
				wellTypeFilter = dialog.getActiveFilterItems();
				setInput();
			}
		});

		// Add Well Status Filter.
		menuItem = new MenuItem(filterMenu, SWT.CASCADE);
		Menu subMenu = new Menu(filterMenu);
		menuItem.setMenu(subMenu);
		menuItem.setText("Well Status Filter");
		createSubMenuItem(subMenu, ACCEPTED);
		createSubMenuItem(subMenu, REJECTED);

		return filterMenu;
	}

	private void createSubMenuItem(Menu parent, String filter) {
		MenuItem menuItem = new MenuItem(parent, SWT.CHECK);
		menuItem.setText(filter);
		menuItem.setSelection(wellStatusFilter.contains(filter));
		menuItem.addListener(SWT.Selection, e -> {
			if (menuItem.getSelection()) wellStatusFilter.add(filter);
			else wellStatusFilter.remove(filter);
			setInput();
		});
	}

}
