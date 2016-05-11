package eu.openanalytics.phaedra.ui.silo.chart.view;

import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.COMPOUND;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.CONTOUR_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.DENSITY_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SCATTER_DENSITY_2D;
import static eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName.SILO_IMAGE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.CompositeChartLegendView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.ui.silo.chart.data.SiloDataProvider;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloClassificationGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloFeatureGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloPlateGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloWellGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.grouping.SiloWellTypeGroupingStrategy;
import eu.openanalytics.phaedra.ui.silo.chart.layer.SiloChartLayerFactory;

public abstract class SiloView extends CompositeChartLegendView<Silo, Silo> {

	private List<Silo> currentSilos;

	private SiloChartLayerFactory chartLayerFactory;

	private List<IGroupingStrategy<Silo, Silo>> groupingStrategies = new ArrayList<IGroupingStrategy<Silo, Silo>>() {
		private static final long serialVersionUID = 2069735350166420014L;
		{
			add(new DefaultGroupingStrategy<Silo, Silo>());
			add(new SiloWellGroupingStrategy());
			add(new SiloPlateGroupingStrategy());
			add(new SiloWellTypeGroupingStrategy());
			add(new SiloFeatureGroupingStrategy());
			add(new SiloClassificationGroupingStrategy());
		}
	};

	private List<ChartName> siloPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 4469735350166420045L;
		{
			add(SCATTER_2D);
			add(DENSITY_2D);
			add(SCATTER_DENSITY_2D);
			add(CONTOUR_2D);
			add(SILO_IMAGE);
			add(COMPOUND);
		}
	};

	@Override
	public ISelectionListener initializeSelectionListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != SiloView.this) {
				Silo silo = SelectionUtils.getFirstObject(selection, Silo.class);
				if (silo != null) {
					List<Silo> silos = new ArrayList<>();
					silos.add(silo);
					siloSelectionChanged(silos);
				}
			}
		};
	}

	@Override
	public ISelectionListener initializeHighlightListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != SiloView.this) {
				siloDataSelectionChanged(selection);
			}
		};
	}

	@Override
	public IModelEventListener initializeModelEventListener() {
		return event -> {
			Silo silo = SelectionUtils.getAsClass(event.source, Silo.class);
			if (silo != null && currentSilos.contains(silo)) {
				JobUtils.runBackgroundJob(monitor -> {
					Display.getDefault().asyncExec(() -> {
						getItemSelectionChangedObservable().valueChanged(currentSilos);
					});
				}, SiloView.this.toString(), null, 1000);
			}
		};
	}

	@Override
	public ChartLayerFactory<Silo, Silo> getChartLayerFactory() {
		if (chartLayerFactory == null) {
			chartLayerFactory = new SiloChartLayerFactory();
		}
		return chartLayerFactory;
	}

	@Override
	public List<IGroupingStrategy<Silo, Silo>> getGroupingStrategies() {
		return groupingStrategies;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return siloPossibleLayers;
	}

	@Override
	public void axisFeatureChanged(String feature) {
		for (AbstractChartLayer<Silo, Silo> layer : getChartView().getChartLayers()) {
			if (layer.getDataProvider() != null) {
				if (layer.getDataProvider().getSelectedFeatures().contains(feature)) {
					((SiloDataProvider) layer.getDataProvider()).removeFeature(feature);
				} else {
					layer.getDataProvider().getSelectedFeatures().add(feature);
					layer.getDataProvider().setDataBounds(null);
				}
				layer.getDataProvider().setFilters(null);
			}
		}
		super.axisFeatureChanged(feature);
	}

	@Override
	public BaseLegendView<Silo, Silo> createLegendView(Composite composite, List<AbstractChartLayer<Silo, Silo>> layers) {
		SiloLegendView legendView = new SiloLegendView(composite, layers, groupingStrategies);
		getItemSelectionChangedObservable().addObserver(legendView.getItemSelectionChangedObservable());
		return legendView;
	}

	private void siloDataSelectionChanged(ISelection selection) {
		List<SubWellSelection> subwells = SubWellUtils.getSubWellSelections(selection);
		if (!subwells.isEmpty()) {
			getItemHighlightChangedObservable().valueChanged(subwells);
		} else if (SelectionUtils.getFirstObject(selection, Well.class) != null) {
			List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
			getItemHighlightChangedObservable().valueChanged(wells);
		}
	}

	private void siloSelectionChanged(List<Silo> silos) {
		if (silos != null && !silos.isEmpty() && !silos.equals(currentSilos)) {
			currentSilos = silos;
			getItemSelectionChangedObservable().valueChanged(silos);
		}
	}

}