package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.openscada.ui.breadcrumbs.BreadcrumbViewer;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewerUtils;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.breadcrumb.BreadcrumbFactory;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class QuickHeatmap extends DecoratedView {

	private BreadcrumbViewer breadcrumb;
	private GridViewer gridViewer;
	private GridLayerSupport gridLayerSupport;

	private Plate currentPlate;

	private ISelectionListener selectionListener;
	private IUIEventListener uiEventListener;
	private IModelEventListener modelEventListener;
	
	@Override
	public void createPartControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		breadcrumb = BreadcrumbFactory.createBreadcrumb(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(breadcrumb.getControl());

		gridViewer = new GridViewer(container);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());

		gridLayerSupport = new GridLayerSupport("hca.singlewell.grid|hca.well.grid", gridViewer);
		gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
		gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getViewSite().getActionBars().getServiceLocator() == null);

		gridViewer.setContentProvider(new PlateContentProvider());
		gridViewer.setLabelProvider(gridLayerSupport.createLabelProvider());

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part == QuickHeatmap.this) return;
				Plate plate = SelectionUtils.getFirstObject(selection, Plate.class);
				if (plate != null && (currentPlate == null || !currentPlate.equals(plate))) {
					currentPlate = plate;
					breadcrumb.setInput(plate);
					breadcrumb.getControl().getParent().layout();
					gridLayerSupport.setInput(plate);
					setPartName(plate.getBarcode() + " - Quick Heatmap");
				}
				List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
				if (wells == null || wells.isEmpty()) {
					List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
					if (compounds != null && !compounds.isEmpty()) {
						wells = new ArrayList<Well>();
						for (Compound c: compounds) wells.addAll(c.getWells());
					}
				}
				if (wells != null && !wells.isEmpty()) {
					gridViewer.setSelection(new StructuredSelection(wells));
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		uiEventListener = event -> {
			if (currentPlate == null) return;

			if (event.type == EventType.FeatureSelectionChanged || event.type == EventType.NormalizationSelectionChanged) {
				ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
				ProtocolClass thisPClass = currentPlate.getExperiment().getProtocol().getProtocolClass();
				if (thisPClass.equals(pClass)) {
					gridLayerSupport.setInput(currentPlate);
				}
			} else if (event.type == EventType.ColorMethodChanged) {
				gridViewer.setInput(gridViewer.getInput());
			} else if (event.type == EventType.ImageSettingsChanged) {
				gridViewer.refresh();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);

		modelEventListener = event -> {
			if (currentPlate == null) return;
			
			if (event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					if (currentPlate.equals((Plate)event.source)) {
						Display.getDefault().asyncExec(() -> gridViewer.setInput(gridViewer.getInput()));
					}
				}
			} else if (event.type == ModelEventType.ValidationChanged) {
				Object[] items = ModelEventService.getEventItems(event);
				List<Plate> plates = SelectionUtils.getAsClass(items, Plate.class);
				if (plates.contains(currentPlate)) {
					Display.getDefault().asyncExec(() -> gridViewer.setInput(gridViewer.getInput()));
				}
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);
		
		getSite().setSelectionProvider(gridViewer);
		
		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ProtocolUIService.getInstance().addUIEventListener(uiEventListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		initDecorators(parent, gridViewer.getControl());

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewQuickHeatmap");
	}

	public void setPlate(Plate plate) {
		selectionListener.selectionChanged(null, new StructuredSelection(plate));
	}

	@Override
	public void setFocus() {
		gridViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
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

	private Protocol getProtocol() {
		return (currentPlate == null) ? null : currentPlate.getExperiment().getProtocol();
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		
		// Retrieve the current state of all active layers.
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			if (layer.isEnabled()) {
				Object layerConfig = layer.getConfig();
				if (layerConfig == null) layerConfig = true;
				properties.addProperty(layer.getName() + GridViewerUtils.CONFIG, layerConfig);
			}
		}
		
		// A Quick Heatmap nearly always contains a feature-specific layer.
		PartSettingsUtils.setFeature(properties, ProtocolUIService.getInstance().getCurrentFeature());
		PartSettingsUtils.setNormalization(properties, ProtocolUIService.getInstance().getCurrentNormalization());
		
		return properties;
	}
	
	private void setProperties(Properties properties) {
		for (IGridLayer layer : gridLayerSupport.getLayers()) {
			Object layerConfig = properties.getProperty(layer.getName() + GridViewerUtils.CONFIG);
			boolean hasLayerConfig = layerConfig != null;
			if (hasLayerConfig) layer.setConfig(layerConfig);
			layer.toggleEnabled(hasLayerConfig);
		}
		
		ProtocolUIService.getInstance().setCurrentFeature(PartSettingsUtils.getFeature(properties));
		ProtocolUIService.getInstance().setCurrentNormalization(PartSettingsUtils.getNormalization(properties));
		
		gridViewer.refresh();
	}
}