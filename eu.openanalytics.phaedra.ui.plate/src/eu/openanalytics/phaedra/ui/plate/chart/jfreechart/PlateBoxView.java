package eu.openanalytics.phaedra.ui.plate.chart.jfreechart;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.charting.v2.ChartSelectionManager;
import eu.openanalytics.phaedra.base.ui.charting.widget.BoxWhiskerChart;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.ui.util.view.ShowSecondaryViewDecorator;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.PlateBoxDataProvider;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class PlateBoxView extends DecoratedView {

	private BoxWhiskerChart<Plate> chart;
	private Label featureLbl;
	private ISelectionListener selectionListener;
	private ISelectionListener highlightListener;
	private IUIEventListener featureListener;

	private List<Plate> currentPlates;
	private ChartSelectionManager selectionProvider;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		featureLbl = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(featureLbl);

		chart = new BoxWhiskerChart<>(parent, SWT.NONE);
		chart.addChartSelectionListener(selectedPlates -> {
			selectionProvider.setSelection(new StructuredSelection(new ArrayList<>(selectedPlates)));
			selectionProvider.fireSelection();
		});
		GridDataFactory.fillDefaults().grab(true,true).applyTo(chart);

		selectionListener = (part, selection) -> {
			if (part == PlateBoxView.this) return;

			List<Plate> plates = getSelectedPlates(selection);
			if (!plates.isEmpty() && !plates.equals(currentPlates)) {
				currentPlates = plates;
				update();
			}
		};

		highlightListener = (part, selection) -> {
			if (part == PlateBoxView.this) return;

			List<Plate> plates = getSelectedPlates(selection);
			if (!plates.isEmpty()) {
				chart.setSelection(plates);
			}
		};

		featureListener = (event) -> {
			if (event.type == EventType.FeatureSelectionChanged) {
				update();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureListener);

		selectionProvider = new ChartSelectionManager();
		getSite().setSelectionProvider(selectionProvider);

		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));

		Menu menu = menuMgr.createContextMenu(chart);
		chart.setMenu(menu);

		getSite().registerContextMenu(menuMgr, selectionProvider);

		addDecorator(new SelectionHandlingDecorator(selectionListener, highlightListener));
		addDecorator(new CopyableDecorator());
		addDecorator(new ShowSecondaryViewDecorator());
		initDecorators(parent);

		// Try to get an initial selection from the page.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewPlateBoxWhisker");
	}

	@Override
	public void setFocus() {
		chart.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureListener);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		super.fillContextMenu(manager);
	}

	@Override
	protected void fillToolbar() {
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				chart.fillToolBar(parent);
			}
		};
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(contributionItem);
		super.fillToolbar();
	}

	private List<Plate> getSelectedPlates(ISelection selection) {
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) {
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			experiments.forEach(exp -> plates.addAll(PlateService.getInstance().getPlates(exp)));
		}
		return plates;
	}

	private void update() {
		checkCurrentFeature();
		if (currentPlates != null) {
			PlateBoxDataProvider dataProvider = new PlateBoxDataProvider(currentPlates);
			chart.setRenderCustomizer(dataProvider.createRenderCustomizer());
			chart.setDataProvider(dataProvider);
		}
	}

	private void checkCurrentFeature() {
		Feature f = ProtocolUIService.getInstance().getCurrentFeature();
		if (f != null) {
			featureLbl.setText("Feature: " + f.getName());
			featureLbl.getParent().layout();
		}
	}

}