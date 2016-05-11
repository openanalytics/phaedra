package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseChartView;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public abstract class SubWell1DView extends SubWellView {

	private ToolItem independantLayerButton;

	private List<ChartName> subWellDensityPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.HISTOGRAM_1D);
			add(ChartName.DENSITY_1D);
			add(ChartName.GATES_1D);
		}
	};

	@Override
	public List<ChartName> getPossibleLayers() {
		return subWellDensityPossibleLayers;
	}

	@Override
	protected void addSpecificToolbarButtons(ToolBar toolbar) {
		super.addSpecificToolbarButtons(toolbar);

		independantLayerButton = new ToolItem(toolbar, SWT.CHECK);
		independantLayerButton.setImage(IconManager.getIconImage("dependent.png"));
		independantLayerButton.setToolTipText("Toggle independant layers");
		independantLayerButton.addListener(SWT.Selection, event -> {
			if (independantLayerButton.getSelection()) {
				for (AbstractChartLayer<Well, Well> layer : getChartView().getChartLayers()) {
					layer.getChartSettings().getMiscSettings().put(BaseChartView.INDEPENDENT_LAYER, "" + true);
					independantLayerButton.setImage(IconManager.getIconImage("independent.png"));
				}
			} else {
				for (AbstractChartLayer<Well, Well> layer : getChartView().getChartLayers()) {
					layer.getChartSettings().getMiscSettings().remove(BaseChartView.INDEPENDENT_LAYER);
					independantLayerButton.setImage(IconManager.getIconImage("dependent.png"));
				}
			}
			JobUtils.runUserJob(
					monitor -> getChartView().recalculateDataBounds(monitor)
					, getPartName() + ": Recalculate Bounds"
					, 100
					, toString()
					, null
			);
		});
	}

	@Override
	protected void doLoadView() {
		super.doLoadView();
		boolean isIndependantLayer = false;
		for (AbstractChartLayer<?, ?> layer : getChartView().getChartLayers()) {
			isIndependantLayer |= layer.getChartSettings().getMiscSettings().get(BaseChartView.INDEPENDENT_LAYER) != null;
		}
		if (independantLayerButton != null) {
			independantLayerButton.setSelection(isIndependantLayer);
			independantLayerButton.setImage(IconManager.getIconImage((isIndependantLayer ? "in" : "") + "dependent.png"));
		}
	}

}
