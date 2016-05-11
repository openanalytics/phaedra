package eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.GATES_1D;

import org.flowcyt.facejava.gating.gates.Gate;
import org.flowcyt.facejava.gating.gates.GateSet;
import org.flowcyt.facejava.gating.gates.geometric.GeometricGate;
import org.flowcyt.facejava.gating.gates.geometric.RectangleGate;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes1DChart;
import uk.ac.starlink.ttools.plot.GatePlot;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.TablePlot;

public class Gates1DChart<ENTITY, ITEM> extends Axes1DChart<ENTITY, ITEM> {

	private GateSet gateSet;

	public Gates1DChart() {
		super();
		setName(GATES_1D);
	}

	@Override
	public TablePlot createPlot() {
		PtPlotSurface plotSurface = new PtPlotSurface();
		plotSurface.setAxes(false);
		plotSurface.setPreserveAxesSpace(isPreserveAxesSpace());
		GatePlot plot = new GatePlot(plotSurface);
		plot.setOpaque(false);
		return plot;
	}

	@Override
	public void updatePlotData() {
		super.updatePlotData();

		getPlotState().setShowAxes(false);
		getPlotState().setGrid(false);

		gateSet = GateFactory.createGateSetFromGateXML(
				getDataProvider().getCurrentEntities().get(0), getDataProvider().getGates());
	}

	@Override
	public void settingsChanged() {
		((GatePlot) getPlot()).setGateSet(getValidGates());
		((GatePlot) getPlot()).setGateSettings(getChartSettings().getGateSettings());

		super.settingsChanged();
	}

	private GateSet getValidGates() {
		// Get the gates that can be displayed on the current chart and feature combination
		GateSet validGates = new GateSet();
		if (gateSet != null) {
			for (Gate gate : gateSet) {
				boolean validGate = false;
				if (gate instanceof RectangleGate) {
					validGate = checkGateDimension(0, 0, gate);
					getChartSettings().getGateSettings().setGateOrientation(gate.getId(), !validGate);
				}
				if (validGate) {
					validGates.add(gate);
				}
			}
		}
		return validGates;
	}

	private boolean checkGateDimension(int chartDimension, int gateDimension, Gate gate) {
		// Gates are compared on the labels because when e.g. a gate is based on the glog value of a feature
		// it would be a field called APC-A Glog in KNIME but in Phaedra it would be the following expression glog($APC-A$).
		// By checking on the Label (which is the Feature name by default) the user can still force the gate to be shown.
		String dimName = ((GeometricGate) gate).getDimensions().get(gateDimension).getValue();
		boolean sameFeature = dimName.equalsIgnoreCase(getDataProvider().getSelectedFeature(chartDimension));
		boolean sameLabel = dimName.equalsIgnoreCase(getDataProvider().getAxisLabels()[chartDimension]);
		return sameLabel || sameFeature;
	}

	public GateSet getGateSet() {
		return gateSet;
	}
}