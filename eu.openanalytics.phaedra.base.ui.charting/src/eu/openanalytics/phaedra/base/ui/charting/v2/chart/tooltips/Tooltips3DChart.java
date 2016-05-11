package eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.CELL_IMAGE_3D;

import java.util.BitSet;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter3DChart;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.plot.Tooltips3DPlot;
import uk.ac.starlink.ttools.plot.TooltipsPlot.ITooltipProvider;

public abstract class Tooltips3DChart<ENTITY, ITEM> extends Scatter3DChart<ENTITY, ITEM> {

	public Tooltips3DChart() {
		super();
		setName(CELL_IMAGE_3D);
	}

	@Override
	public TablePlot createPlot() {
		return new Tooltips3DPlot();
	}

	@Override
	public void setSelection(BitSet bitSet) {
		if (bitSet != null) {
			// Generate the tooltips based on the bitset
			ITooltipProvider tooltipProvider = getTooltipProvider();
			tooltipProvider.setConfig(getChartSettings().getTooltipSettings());
			((Tooltips3DPlot) getPlot()).setTooltipProvider(tooltipProvider);
		}

		super.setSelection(bitSet);
	}

	@Override
	public void settingsChanged() {
		((Tooltips3DPlot) getPlot()).setFontSize(getChartSettings().getTooltipSettings().getFontSize());
		((Tooltips3DPlot) getPlot()).setShowLabels(getChartSettings().getTooltipSettings().isShowLabels());
		((Tooltips3DPlot) getPlot()).setShowCoords(getChartSettings().getTooltipSettings().isShowCoords());
		// Set additional tooltip settings.
		ITooltipProvider tooltipProvider = ((Tooltips3DPlot) getPlot()).getTooltipProvider();
		if (tooltipProvider != null) {
			tooltipProvider.setConfig(getChartSettings().getTooltipSettings());
		}
		super.settingsChanged();
	}

	protected abstract ITooltipProvider getTooltipProvider();

}