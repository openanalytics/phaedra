package eu.openanalytics.phaedra.ui.curve.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedEditor;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.MultiploCompound;
import eu.openanalytics.phaedra.ui.curve.grid.provider.CompoundContentProvider;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class CompoundGridEditor extends DecoratedEditor {

	private CTabFolder tabFolder;
	private CTabItem curveTab;
	private CTabItem imageTab;

	private CompoundGrid curveGrid;
	private CompoundImageGrid imageGrid;

	private List<Compound> compounds;
	private boolean imageTabInitialized;

	private SelectionProviderIntermediate selectionProvider;
	private ISelectionListener selectionListener;
	private IModelEventListener curveFitListener;

	private MenuManager menuMgr;
	private ToolBarManager mgr;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).applyTo(parent);

		loadCompounds();
		List<Feature> features = new ArrayList<>();
		if (!compounds.isEmpty()) features = CollectionUtils.findAll(ProtocolUtils.getFeatures(compounds.get(0)), ProtocolUtils.FEATURES_WITH_CURVES);

		menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		mgr = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = mgr.createControl(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBar);

		tabFolder = new CTabFolder(parent, SWT.BOTTOM | SWT.V_SCROLL | SWT.H_SCROLL);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
		tabFolder.addListener(SWT.Selection, e -> {
			Widget w = e.item;
			tabChanged(w);
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		curveTab = new CTabItem(tabFolder, SWT.NONE);
		curveTab.setText("CRC View");

		curveGrid = new CompoundGrid(tabFolder, compounds, features, menuMgr);
		curveTab.setControl(curveGrid);

		imageTab = new CTabItem(tabFolder, SWT.NONE);
		imageTab.setText("Image View");

		tabFolder.setSelection(curveTab);

		selectionListener = (part, selection) -> {
			if (CompoundGridEditor.this == part) return;

			if (tabFolder.getSelection() == curveTab) curveGrid.preSelection();
			else imageGrid.preSelection();
			selectionProvider.setSelection(selection);
			if (tabFolder.getSelection() == curveTab) curveGrid.postSelection();
			else imageGrid.postSelection();
		};
		getSite().getPage().addSelectionListener(selectionListener);

		// Listen to Curve fit events: refresh the chart if that happens.
		curveFitListener = event -> {
			if ((event.type == ModelEventType.CurveFit || event.type == ModelEventType.CurveFitFailed) && event.source instanceof Curve) {
				Display.getDefault().asyncExec(() -> curveGrid.redraw());
			} else if (event.type == ModelEventType.ValidationChanged) {
				Display.getDefault().asyncExec(() -> curveGrid.redraw());
			}
		};
		ModelEventService.getInstance().addEventListener(curveFitListener);

		selectionProvider = new SelectionProviderIntermediate();
		selectionProvider.addSelectionChangedListener(event -> {
			// Propagate the selection to the other tab.
			if (!imageTabInitialized) return;
			if (event.getSource() == curveGrid.getSelectionProvider()) {
				imageGrid.preSelection();
				imageGrid.getSelectionProvider().setSelection(event.getSelection());
				imageGrid.postSelection();
			} else if (event.getSource() == imageGrid.getSelectionProvider()) {
				curveGrid.preSelection();
				curveGrid.getSelectionProvider().setSelection(event.getSelection());
				curveGrid.postSelection();
			}
		});
		selectionProvider.setSelectionProviderDelegate(curveGrid.getSelectionProvider());
		getSite().setSelectionProvider(selectionProvider);
		// Register menu manager with selection provider.
		getEditorSite().registerContextMenu(menuMgr, selectionProvider);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		initDecorators(parent);
		
		ContributionItem item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				if (tabFolder.getSelection() == curveTab) curveGrid.createButtons(parent);
				else imageGrid.createButtons(parent);
			}
		};
		mgr.add(item);
		mgr.update(true);
	}

	@Override
	public void setFocus() {
		curveGrid.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(curveFitListener);
		curveGrid.dispose();
		if (imageGrid != null) imageGrid.dispose();
		super.dispose();
	}

	/**
	 * TODO Should be removed, but it's used (in a dirty way) by CompoundGridEditorContentProvider
	 */
	public List<Compound> getCompounds() {
		return compounds;
	}
	
	/*
	 * ****************
	 * Loading & saving
	 * ****************
	 */

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Do nothing.
	}

	@Override
	public void doSaveAs() {
		// Do nothing.
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	@Override
	protected IToolBarManager getToolBarManager() {
		return mgr;
	}
	
	private void loadCompounds() {
		VOEditorInput input = (VOEditorInput)getEditorInput();
		compounds = new ArrayList<>();
		List<Feature> features = null;
		
		for (IValueObject vo: input.getValueObjects()) {
			Compound comp = (Compound) vo;
			Compound compoundToAdd = null;
			
			if (features == null) features = CollectionUtils.findAll(ProtocolUtils.getFeatures(comp), ProtocolUtils.FEATURES_WITH_CURVES);
			
			// First, translate compound to multiplo compound, if needed.
			List<Compound> multiploCompounds = CalculationService.getInstance().getMultiploCompounds(comp);
			if (multiploCompounds.size() > 1) {
				Compound firstCompound = multiploCompounds.stream()
						.filter(c -> !CompoundValidationStatus.INVALIDATED.matches(c))
						.filter(c -> !PlateValidationStatus.INVALIDATED.matches(c.getPlate()))
						.findFirst().orElse(multiploCompounds.get(0));
				Compound match = compounds.stream()
						.filter(c -> c instanceof MultiploCompound)
						.map(c -> (MultiploCompound) c)
						.filter(c -> c.getCompounds().contains(comp))
						.findAny().orElse(null);
				if (match == null) compoundToAdd = new MultiploCompound(firstCompound, multiploCompounds);
			} else {
				compoundToAdd = comp;
			}
			
			// Then, look up the compound's groupings, if any.
			Set<CurveGrouping> groupings = new HashSet<>();
			for (Feature f: features) {
				for (CurveGrouping cg: CurveService.getInstance().getGroupings(compoundToAdd, f)) groupings.add(cg);
			}
			for (CurveGrouping grouping: groupings) {
				compounds.add(new CompoundWithGrouping(compoundToAdd, grouping));
			}
		}
		
		setPartName(compounds.size() + " compounds");
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void tabChanged(Widget tab) {
		// Update the selection provider intermediate.
		if (tab == curveTab) {
			selectionProvider.setSelectionProviderDelegate(curveGrid.getSelectionProvider());
		} else if (tab == imageTab) {
			if (!imageTabInitialized) {
				imageTabInitialized = true;
				imageGrid = new CompoundImageGrid(tabFolder, compounds, menuMgr);
				imageTab.setControl(imageGrid);
				imageGrid.startPreLoading();
			}
			selectionProvider.setSelectionProviderDelegate(imageGrid.getSelectionProvider());
		}
	}

	private Protocol getProtocol() {
		return compounds.stream().findAny().map(c -> (Protocol)c.getAdapter(Protocol.class)).orElse(null);
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty("ACTIVE_TAB", tabFolder.getSelectionIndex());

		// Save provider settings.
		curveGrid.getCompoundContentProvider().saveSettings(properties);
		if (imageTabInitialized) imageGrid.getCompoundImageContentProvider().saveSettings(properties);

		// Save table settings.
		java.util.Properties tableProperties = new java.util.Properties();
		curveGrid.getTable().saveState("SETTINGS_TABLE", tableProperties);
		properties.addProperty("SETTINGS_TABLE", tableProperties);
		if (imageTabInitialized) {
			tableProperties = new java.util.Properties();
			imageGrid.getTable().saveState("SETTINGS_IMAGE_TABLE", tableProperties);
			properties.addProperty("SETTINGS_IMAGE_TABLE", tableProperties);
		}
		return properties;
	}
	
	private void setProperties(Properties properties) {
		tabFolder.setSelection(properties.getProperty("ACTIVE_TAB", 0));
		tabChanged(tabFolder.getSelection());

		// Load provider settings.
		curveGrid.getCompoundContentProvider().loadSettings(properties);
		if (imageTabInitialized) imageGrid.getCompoundImageContentProvider().loadSettings(properties);

		// Save table settings.
		java.util.Properties tableProperties = properties.getProperty("SETTINGS_TABLE", java.util.Properties.class);
		if (tableProperties != null) {
			curveGrid.getTable().loadState("SETTINGS_TABLE", tableProperties);
		} else {
			// Support for pre 3.0 Saved Views.
			@SuppressWarnings("unchecked")
			Map<Integer,String> columnState = (Map<Integer, String>) properties.getProperty("SETTINGS_COLUMNSTATE");
			if (columnState != null) {
				CompoundContentProvider ccp = curveGrid.getCompoundContentProvider();
				for (int i = 0; i < ccp.getColumnCount(); i++) {
					String state = columnState.get(i);
					if (state == null) continue;
					String[] states = state.split("#");
					boolean hidden = Boolean.valueOf(states[0]);
					if (hidden) NatTableUtils.hideColumn(curveGrid.getTable(), i);
					int width = Integer.valueOf(states[1]);
					NatTableUtils.resizeColumn(curveGrid.getTable(), i, width);
				}
			}
		}
		if (imageTabInitialized) {
			tableProperties = properties.getProperty("SETTINGS_IMAGE_TABLE", new java.util.Properties());
			imageGrid.getTable().loadState("SETTINGS_IMAGE_TABLE", tableProperties);
		}
	}
}