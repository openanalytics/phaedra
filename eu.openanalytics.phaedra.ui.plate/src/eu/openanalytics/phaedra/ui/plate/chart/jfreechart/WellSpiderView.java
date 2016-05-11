package eu.openanalytics.phaedra.ui.plate.chart.jfreechart;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.charting.widget.SpiderChart;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingMode;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.ui.util.view.ShowSecondaryViewDecorator;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;
import eu.openanalytics.phaedra.ui.plate.chart.jfreechart.data.WellSpiderDataProvider;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;

public class WellSpiderView extends DecoratedView {

	private SpiderChart<Well> chart;
	private ISelectionListener selectionListener;
	private IUIEventListener normalizationListener;

	private Well currentWell;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		chart = new SpiderChart<Well>(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);

		selectionListener = (part, selection) -> {
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null && !well.equals(currentWell)) {
				boolean sameProtocolClass = true;
				if(currentWell == null || currentWell.getPlate().getExperiment().getProtocol().getProtocolClass() != well.getPlate().getExperiment().getProtocol().getProtocolClass()) sameProtocolClass = false;
				currentWell = well;
				chart.setDataProvider(new WellSpiderDataProvider(well), sameProtocolClass);
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		normalizationListener = event -> {
			if (event.type == EventType.NormalizationSelectionChanged) {
				chart.setDataProvider(new WellSpiderDataProvider(currentWell));
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(normalizationListener);

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				chart.createButtons(parent);
			}
		};
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(contributionItem);

		addDecorator(new SelectionHandlingDecorator(selectionListener) {
			@Override
			protected void handleModeChange(SelectionHandlingMode newMode) {
				super.handleModeChange(newMode);
				ProtocolUIService.getInstance().removeUIEventListener(normalizationListener);
				if (newMode == SelectionHandlingMode.SEL_HILITE) {
					ProtocolUIService.getInstance().addUIEventListener(normalizationListener);
				}
			}
		});
		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new CopyableDecorator());
		addDecorator(new ShowSecondaryViewDecorator());
		initDecorators(parent);

		// Try to get an initial selection from the page.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewWellSpiderPlot");
	}

	@Override
	public void setFocus() {
		chart.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ProtocolUIService.getInstance().removeUIEventListener(normalizationListener);
		super.dispose();
	}
	
	private Protocol getProtocol() {
		if (currentWell == null) return null;
		return (Protocol) currentWell.getAdapter(Protocol.class);
	}
	
	private Properties getProperties() {
		return chart.getProperties();
	}
	
	private void setProperties(Properties properties) {
		chart.setProperties(properties);
	}
}
