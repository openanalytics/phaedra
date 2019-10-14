package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;

import eu.openanalytics.phaedra.base.datatype.util.DataFormatSupport;
import eu.openanalytics.phaedra.base.ui.gridviewer.advanced.AdvancedGridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.advanced.ModifyableGridContentProvider;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridLayerSupport;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.ui.util.view.ShowSecondaryViewDecorator;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.util.WellUtils;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class WellGridView extends DecoratedView {

	private DataFormatSupport dataFormatSupport;
	
	private AdvancedGridViewer gridViewer;
	private GridLayerSupport gridLayerSupport;

	private List<Well> currentWells;

	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IUIEventListener uiEventListener;

	@Override
	public void createPartControl(Composite parent) {
		this.dataFormatSupport = new DataFormatSupport(this::reloadData);
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		gridViewer = new AdvancedGridViewer(container, 0, 0);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(gridViewer.getControl());

		gridLayerSupport = new GridLayerSupport("hca.singlewell.grid|hca.well.grid", gridViewer, dataFormatSupport);
		gridLayerSupport.setAttribute("featureProvider", ProtocolUIService.getInstance());
		gridLayerSupport.setAttribute(GridLayerSupport.IS_HIDDEN, getViewSite().getActionBars().getServiceLocator() == null);

		gridViewer.setContentProvider(new ModifyableGridContentProvider());
		gridViewer.setLabelProvider(gridLayerSupport.createLabelProvider());
		gridViewer.setInput(Arrays.asList(new Well[] { null, null, null, null, null }));
		gridViewer.setSelectionConfiguration(ConfigurableStructuredSelection.NO_PARENT);

		selectionListener = (part, selection) -> {
			if (part == WellGridView.this) return;

			List<Well> wells = WellUtils.getWells(selection);
			setInput(wells);
		};
		getSite().getPage().addSelectionListener(selectionListener);

		highlightListener = (part, selection) -> {
			if (part == WellGridView.this) return;

			List<Well> wells = WellUtils.getWells(selection);
			gridViewer.setSelection(new StructuredSelection(wells));
		};
		getSite().getPage().addSelectionListener(highlightListener);

		uiEventListener = event -> {
			if (currentWells == null || currentWells.isEmpty()) return;

			if (event.type == EventType.FeatureSelectionChanged || event.type == EventType.NormalizationSelectionChanged) {
				ProtocolClass pClass = ProtocolUIService.getInstance().getCurrentProtocolClass();
				ProtocolClass thisPClass = currentWells.get(0).getPlate().getExperiment().getProtocol().getProtocolClass();
				if (thisPClass.equals(pClass)) {
					gridLayerSupport.setInput(gridViewer.getInput());
				}
			} else if (event.type == EventType.ColorMethodChanged) {
				gridViewer.setInput(gridViewer.getInput());
			} else if (event.type == EventType.ImageSettingsChanged) {
				gridViewer.refresh();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);

		getSite().setSelectionProvider(gridViewer);

		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
				if (newMode == SelectionHandlingMode.SEL) {
					ProtocolUIService.getInstance().addUIEventListener(uiEventListener);
				}
			}
		});
		addDecorator(new CopyableDecorator());
		addDecorator(new ShowSecondaryViewDecorator());
		initDecorators(parent, gridViewer.getControl());

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);
	}

	@Override
	public void setFocus() {
		gridViewer.getControl().setFocus();
	}
	
	private void reloadData() {
		final List<Well> input;
		if (this.gridViewer == null || this.gridViewer.getControl().isDisposed()
				|| (input = this.currentWells) == null) {
			return;
		}
		this.gridLayerSupport.setInput(input);
	}

	@Override
	public void dispose() {
		if (this.dataFormatSupport != null) this.dataFormatSupport.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		getSite().getPage().removeSelectionListener(highlightListener);
		ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
		super.dispose();
	}

	@Override
	protected void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mgr = bars.getToolBarManager();

		mgr.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem item = new ToolItem(parent, SWT.PUSH);
				item.setImage(IconManager.getIconImage("settings.gif"));
				item.setToolTipText("Change the settings for the grid");
				item.addListener(SWT.Selection, e -> {
					Dialog dialog = gridViewer.createDialog();
					dialog.open();
				});
			}
		});
		super.fillToolbar();
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

	private void setInput(List<Well> wells) {
		if (!wells.isEmpty() && !wells.equals(currentWells)) {
			currentWells = wells;
			gridLayerSupport.setInput(currentWells);
			setTitleToolTip(getPartName() + "\nNr. of Items: " + currentWells.size());
		}
	}

}
