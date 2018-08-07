package eu.openanalytics.phaedra.ui.plate.chart.svg;

import java.io.IOException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.ui.util.view.ShowSecondaryViewDecorator;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;

public class SVGChartView extends DecoratedView {

	public final static String PROP_CURRENT_CHART_INDEX = "currentChartIndex";
	
	private Combo chartCombo;
	private Canvas chartCanvas;
	private ISelectionListener selectionListener;

	private Well currentWell;
	private Image currentChart;
	private int currentChartIndex;

	private SVGChartSupport chartSupport;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(parent);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Chart:");

		chartCombo = new Combo(parent, SWT.READ_ONLY);
		chartCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentChartIndex = chartCombo.getSelectionIndex();
				createChart();
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(chartCombo);

		chartCanvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.BORDER);
		chartCanvas.addPaintListener(e -> paintChart(e.gc));
		chartCanvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				createChart();
			}
		});
		GridDataFactory.fillDefaults().span(2,1).grab(true,true).applyTo(chartCanvas);

		selectionListener = (part, selection) -> {
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null && !well.equals(currentWell)) {
				if (currentWell == null) {
					initChartSupport(well);
				} else {
					boolean samePlate = well.getPlate().equals(currentWell.getPlate());
					boolean samePclass = PlateUtils.isSameProtocolClass(well, currentWell);
					if (!samePclass || !samePlate) initChartSupport(well);
				}
				currentWell = well;
				createChart();
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		addDecorator(new ShowSecondaryViewDecorator());
		initDecorators(parent);

		// Obtain an initial selection.
		SelectionUtils.triggerActiveSelection(selectionListener);


		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewSVGChart");
	}

	@Override
	public void setFocus() {
		chartCanvas.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		if (chartSupport != null)
			chartSupport.dispose();
		super.dispose();
	}

	private void initChartSupport(Well well) {
		if (chartSupport != null)
			chartSupport.dispose();
		chartSupport = new SVGChartSupport(well.getPlate());
		chartSupport.setBgColor(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		chartCombo.setItems(chartSupport.getAvailableCharts());
		chartCombo.select(currentChartIndex);
	}

	private void createChart() {
		if (currentWell == null) return;
		if (chartCanvas.getSize().x == 0 || chartCanvas.getSize().y == 0) return;
		if (currentChartIndex == -1 || currentChartIndex >= chartCombo.getItemCount()) return;

		String chartName = chartCombo.getItem(currentChartIndex);
		Image previousChart = currentChart;
		try {
			currentChart = chartSupport.getChart(chartName, currentWell, chartCanvas.getSize().x, chartCanvas.getSize().y);
		} catch (IOException e) {
			currentChart = null;
		}
		if (previousChart != null && !previousChart.isDisposed()) previousChart.dispose();
		chartCanvas.redraw();
	}

	private void paintChart(GC gc) {
		if (currentChart != null && !currentChart.isDisposed()) {
			gc.drawImage(currentChart, 0, 0);
		}
	}
	
	private Protocol getProtocol() {
		return (currentWell == null) ? null : currentWell.getPlate().getExperiment().getProtocol();
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty(PROP_CURRENT_CHART_INDEX, currentChartIndex);
		return properties;
	}
	
	private void setProperties(Properties properties) {
		Integer index = properties.getProperty(PROP_CURRENT_CHART_INDEX, Integer.class);
		if (index != null) currentChartIndex = index;
		chartCombo.select(currentChartIndex);
		createChart();
	}
}
