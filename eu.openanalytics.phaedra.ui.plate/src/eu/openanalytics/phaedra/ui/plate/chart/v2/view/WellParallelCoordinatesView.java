package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellParallelCoordinatesView extends WellView {

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.PARALLEL_COORDINATES);
		}
	};

	@Override
	public List<AbstractChartLayer<Plate, Well>> createChartLayers() {
		List<AbstractChartLayer<Plate, Well>> chartLayers = new ArrayList<AbstractChartLayer<Plate, Well>>();
		chartLayers.add(createLayer(ChartName.AXES_DYNAMIC));
		chartLayers.add(createLayer(ChartName.PARALLEL_COORDINATES));
		chartLayers.add(createLayer(ChartName.SELECTION));
		return chartLayers;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}

	@Override
	protected DropTarget createDropTarget(Control control) {
		DropTarget dropTarget = new DropTarget(control, DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getTransfer()});
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
				// Get selected Features
				List<Feature> features = SelectionUtils.getObjects(sel, Feature.class);
				// Create new List
				List<String> featureNames = new ArrayList<>();
				// Replace current Features with new selected Features starting from 0
				for (int i = 0; i < features.size(); i++) {
					featureNames.add(features.get(i).getDisplayName());
				}
				// Apply the new Feature list to all the data layers
				for (AbstractChartLayer<Plate, Well> layer : getChartView().getChartLayers()) {
					if (layer.isDataLayer()) {
						layer.getDataProvider().setSelectedFeatures(featureNames);
						layer.getDataProvider().setDataBounds(null);
					}
				}
				// Redraw the layers
				getChartView().dataChangedForAllLayers();
				getChartView().recalculateDataBounds();
			}
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)
						&& SelectionUtils.getObjects(LocalSelectionTransfer.getTransfer().getSelection(), Feature.class).size() > 2) {
					event.detail = DND.DROP_LINK;
				}
			}
		});
		return dropTarget;
	}

}