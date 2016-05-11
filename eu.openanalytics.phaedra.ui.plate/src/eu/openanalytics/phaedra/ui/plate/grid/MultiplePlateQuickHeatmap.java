package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewerUtils;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.plate.grid.layer.HeatmapLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.MultiFeatureLayer;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class MultiplePlateQuickHeatmap extends DecoratedView {

	public static final String STAT = "STAT";

	private GridViewer gridViewer;
	private GridLayerSupport gridLayerSupport;
	private ToolItem statDropdown;

	private List<Plate> currentPlates;

	private ISelectionListener selectionListener;
	private IUIEventListener featureSelectionListener;

	@Override
	public void createPartControl(Composite parent) {
		gridViewer = new GridViewer(parent);
		gridViewer.getGrid().setSelectionEnabled(false);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());

		gridLayerSupport = new GridLayerSupport("hca.multiplewell.grid|hca.well.grid", gridViewer);
		gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
		gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getViewSite().getActionBars().getServiceLocator() == null);

		gridViewer.setContentProvider(new PlatesContentProvider());
		gridViewer.setLabelProvider(gridLayerSupport.createLabelProvider());

		selectionListener = (part, selection) ->  {
			if (part == MultiplePlateQuickHeatmap.this) return;

			List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
			if (plates.isEmpty()) {
				List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
				for (Experiment exp : experiments)
					plates.addAll(PlateService.getInstance().getPlates(exp));
			}
			if (!plates.isEmpty() && (currentPlates == null || !currentPlates.equals(plates))) {
				currentPlates = plates;
				gridLayerSupport.setInput(plates);
				setPartName(plates.size() + " Plate(s) - Multi Plate Heatmap");
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		featureSelectionListener = (event) -> {
			if (currentPlates == null || currentPlates.isEmpty()) return;
			ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
			ProtocolClass thisPClass = (ProtocolClass) currentPlates.get(0).getAdapter(ProtocolClass.class);
			if (thisPClass.equals(pClass)) {
				if (event.type == EventType.FeatureSelectionChanged
						|| event.type == EventType.NormalizationSelectionChanged) {
					gridLayerSupport.setInput(currentPlates);
				} else if (event.type == EventType.ColorMethodChanged) {
					gridViewer.setInput(gridViewer.getInput());
				}
			} else {
				// Not interested in selections from another protocol class
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureSelectionListener);

		getSite().setSelectionProvider(gridViewer);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ProtocolUIService.getInstance().removeUIEventListener(featureSelectionListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ProtocolUIService.getInstance().addUIEventListener(featureSelectionListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		initDecorators(parent, gridViewer.getControl());

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewMultiplePlateQuickHeatmap");
	}

	@Override
	public void setFocus() {
		gridViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		gridLayerSupport.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureSelectionListener);
		super.dispose();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		// Contributions are added here.
		manager.add(new Separator());
		gridLayerSupport.contributeContextMenu(manager);
		manager.add(new Separator());
		super.fillContextMenu(manager);
	}

	@Override
	protected void fillToolbar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				statDropdown = DropdownToolItemFactory.createDropdown(parent);
				statDropdown.setImage(IconManager.getIconImage("sum.png"));
				statDropdown.setToolTipText("The statistic that will be used");

				Listener listener = (event) -> {
					MenuItem selected = (MenuItem) event.widget;
					if (!selected.getSelection()) return;
					gridLayerSupport.setStat(selected.getText());
				};

				for (String stat : StatService.getInstance().getAvailableStats()) {
					MenuItem item = DropdownToolItemFactory.createChild(statDropdown, stat, SWT.RADIO);
					item.addListener(SWT.Selection, listener);
					if (stat.equals(gridLayerSupport.getStat())) item.setSelection(true);
				}
			}
		};
		tbm.add(contributionItem);

		super.fillToolbar();
	}

	private Protocol getProtocol() {
		if (currentPlates.isEmpty()) return null;
		return currentPlates.get(0).getExperiment().getProtocol();
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			if (layer.isEnabled()) {
				Object layerConfig = layer.getConfig();
				if (layerConfig == null) layerConfig = true;
				properties.addProperty(layer.getName() + GridViewerUtils.CONFIG, layerConfig);
			}
		}
		properties.addProperty(STAT, gridLayerSupport.getStat());

		if (hasFeatureLayer()) {
			Feature feature = ProtocolUIService.getInstance().getCurrentFeature();
			String norm = ProtocolUIService.getInstance().getCurrentNormalization();
			PartSettingsUtils.setFeature(properties, feature);
			PartSettingsUtils.setNormalization(properties, norm);
		}
		return properties;
	}

	private void setProperties(Properties properties) {
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			Object layerConfig = properties.getProperty(layer.getName() + GridViewerUtils.CONFIG);
			boolean hasLayerConfig = layerConfig != null;
			if (hasLayerConfig) layer.setConfig(layerConfig);
			layer.toggleEnabled(hasLayerConfig);
		}
		gridLayerSupport.setStat(properties.getProperty(STAT, gridLayerSupport.getStat()));
		for (MenuItem i: DropdownToolItemFactory.getMenu(statDropdown).getItems()) {
			i.setSelection(i.getText().equals(gridLayerSupport.getStat()));
		}
		
		// ProtocolUIService will not respond to 'null' values being set, no 'null' check needed.
		ProtocolUIService.getInstance().setCurrentFeature(PartSettingsUtils.getFeature(properties));
		ProtocolUIService.getInstance().setCurrentNormalization(PartSettingsUtils.getNormalization(properties));

		gridViewer.getGrid().redraw();
	}

	private boolean hasFeatureLayer() {
		return GridViewerUtils.hasGridLayerEnabled(gridLayerSupport, HeatmapLayer.class, MultiFeatureLayer.class);
	}


}