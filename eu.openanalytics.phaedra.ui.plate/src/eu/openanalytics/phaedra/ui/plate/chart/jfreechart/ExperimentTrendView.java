package eu.openanalytics.phaedra.ui.plate.chart.jfreechart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;

import eu.openanalytics.phaedra.base.ui.charting.v2.ChartSelectionManager;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.ExperimentTrendControlDataProvider;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.ExperimentTrendStatisticDataProvider;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class ExperimentTrendView extends DecoratedView {

	private Label featureLbl;
	private CombinedTrendChart<Plate> chart;

	private ISelectionListener selectionListener;
	private IUIEventListener featureListener;
	private ChartSelectionManager selectionProvider;

	private Feature currentFeature;
	private List<Plate> currentPlates;
	private List<Plate> currentPlateSelection;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		this.currentPlates = new ArrayList<>();
		this.currentPlateSelection = new ArrayList<>();

		featureLbl = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).applyTo(featureLbl);

		chart = new CombinedTrendChart<Plate>(parent, SWT.NONE);
		chart.addChartSelectionListener(selectedEntities -> {
			currentPlateSelection.clear();
			currentPlateSelection.addAll(selectedEntities);
			highlightPlateSelection();
			selectionProvider.setSelection(new StructuredSelection(currentPlateSelection));
			selectionProvider.fireSelection();
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

		createListeners();

		selectionProvider = new ChartSelectionManager();
		getSite().setSelectionProvider(selectionProvider);

		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(chart);
		chart.setMenu(menu);
		getSite().registerContextMenu(menuMgr, selectionProvider);

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				chart.createToolBarButtons(parent);
			}
			public boolean isDynamic() {
				return true;
			};
		};
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(contributionItem);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new CopyableDecorator());
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		initDecorators(parent);
		
		// Try to get an initial selection from the page.
		SelectionUtils.triggerActiveSelection(selectionListener);
	}

	@Override
	public void setFocus() {
		chart.setFocus();
	}

	@Override
	public void dispose() {
		chart.dispose();
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(featureListener);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		super.fillContextMenu(manager);
	}

	private void createListeners() {
		selectionListener = (part, selection) -> {
			if (part == this) return;
			
			// New plate input
			List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
			List<Plate> plates = experiments.stream()
					.flatMap(e -> PlateService.getInstance().getPlates(e).stream())
					.collect(Collectors.toList());
			Collections.sort(plates, PlateUtils.EXP_ID_PLATE_SEQ_SORTER);
			if (!plates.isEmpty() && !plates.equals(currentPlates)) {
				currentPlates = plates;
				update();
			}
			
			// Highlight plates
			plates = SelectionUtils.getObjects(selection, Plate.class);
			if (!plates.isEmpty() && !plates.equals(currentPlateSelection)) {
				currentPlateSelection = plates;
				highlightPlateSelection();
			}
		};

		featureListener = event -> {
			if (event.type == EventType.FeatureSelectionChanged) {
				Feature f = ProtocolUIService.getInstance().getCurrentFeature();
				if (f != null && !f.equals(currentFeature)) {
					currentFeature = f;
					featureLbl.setText("Feature: " + currentFeature.getName());
					update();
					highlightPlateSelection();
				}
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(featureListener);
	}

	private void update() {
		if (!currentPlates.isEmpty()) {
			if (currentFeature == null) {
				currentFeature = ProtocolUIService.getInstance().getCurrentFeature();
				featureLbl.setText("Feature: " + currentFeature.getName());
			}
			ExperimentTrendControlDataProvider dataProvider = new ExperimentTrendControlDataProvider(currentPlates, currentFeature);
			ExperimentTrendStatisticDataProvider statisticProvider = new ExperimentTrendStatisticDataProvider(currentPlates, currentFeature);
			chart.setRenderCustomizers(dataProvider.createRenderCustomizer(), statisticProvider.createRenderCustomizer());
			chart.setDataProviders(dataProvider, statisticProvider);
			chart.getParent().layout();
		}
	}

	private void highlightPlateSelection() {
		chart.getPlot().clearDomainMarkers();
		for (Plate plate : currentPlateSelection) {
			int ix = currentPlates.indexOf(plate);
			if (ix >= 0) {
				ix++;
				Marker marker = new IntervalMarker(ix - 0.5, ix + 0.5);
				float alpha = 0.4f;
				marker.setAlpha(alpha);
				marker.setPaint(Color.yellow);
				chart.getPlot().addDomainMarker(marker);
			}
		}
	}

	private Protocol getProtocol() {
		if (currentPlates == null) return null;
		return currentPlates.stream().findAny().map(p -> p.getExperiment().getProtocol()).orElse(null);
	}

	private Properties getProperties() {
		Properties properties = chart.getProperties();
		PartSettingsUtils.setFeature(properties, currentFeature);
		return properties;
	}

	private void setProperties(Properties properties) {
		currentFeature = PartSettingsUtils.getFeature(properties);
		chart.setProperties(properties);
		update();
		highlightPlateSelection();
	}
}