package eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.GATES;

import org.flowcyt.facejava.gating.gates.Gate;
import org.flowcyt.facejava.gating.gates.GateSet;
import org.flowcyt.facejava.gating.gates.geometric.GeometricGate;
import org.flowcyt.facejava.gating.gates.geometric.PolygonGate;
import org.flowcyt.facejava.gating.gates.geometric.RectangleGate;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes.Axes2DChart;
import uk.ac.starlink.ttools.plot.GatePlot;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.TablePlot;

public class GatesChart<ENTITY, ITEM> extends Axes2DChart<ENTITY, ITEM> {

	private GateSet gateSet;

	public GatesChart() {
		super();
		setName(GATES);
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
					// Rectangle gate must match one of the dimensions.
					boolean validHorizontal = checkGateDimension(1, 0, gate);
					boolean validVertical = checkGateDimension(0, 0, gate);
					validGate = validHorizontal || validVertical;
					getChartSettings().getGateSettings().setGateOrientation(gate.getId(), validHorizontal);
				} else if (gate instanceof PolygonGate) {
					// Polygon gate must match both dimensions.
					validGate = checkGateDimension(0, 0, gate) && checkGateDimension(1, 1, gate);
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