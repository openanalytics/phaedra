package eu.openanalytics.phaedra.ui.subwell.chart.v2.view;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChart.ChartName;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.IGroupingStrategy;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.ChartLayerFactory;
import eu.openanalytics.phaedra.base.ui.charting.v2.view.BaseLegendView;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;
import eu.openanalytics.phaedra.ui.plate.chart.v2.view.ClassificationLegendView;
import eu.openanalytics.phaedra.ui.plate.chart.v2.view.JEPView;
import eu.openanalytics.phaedra.ui.plate.classification.BaseClassificationSupport;
import eu.openanalytics.phaedra.ui.subwell.SubWellClassificationSupport;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.data.SubWellDataProvider;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.ClassificationGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.CompConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.CompGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.ConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.ExperimentGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.PlateGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.WellGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.WellStatusGroupingStrategy;
import eu.openanalytics.phaedra.ui.subwell.chart.v2.grouping.WellTypeGroupingStrategy;

public abstract class SubWellView extends JEPView<Well, Well> {

	private List<Well> currentWells;
	private SubWellChartLayerFactory chartLayerFactory;

	private ToolItem plateLimitsButton;

	private Job currentJob;

	private List<IGroupingStrategy<Well, Well>> groupingStrategies = new ArrayList<IGroupingStrategy<Well, Well>>() {
		private static final long serialVersionUID = -1016582051043014539L;
		{
			add(new DefaultGroupingStrategy<Well, Well>());
			add(new ExperimentGroupingStrategy());
			add(new PlateGroupingStrategy());
			add(new ClassificationGroupingStrategy());
			add(new WellGroupingStrategy());
			add(new WellStatusGroupingStrategy());
			add(new WellTypeGroupingStrategy());
			add(new CompGroupingStrategy());
			add(new ConcGroupingStrategy());
			add(new CompConcGroupingStrategy());
		}
	};

	private List<ChartName> subWellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.SCATTER_2D);
			add(ChartName.DENSITY_2D);
			add(ChartName.SCATTER_DENSITY_2D);
			add(ChartName.CONTOUR_2D);
			add(ChartName.CELL_IMAGE);
			add(ChartName.COMPOUND);
			add(ChartName.GATES);
		}
	};

	@Override
	public ISelectionListener initializeSelectionListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != SubWellView.this) {
				List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
				if (wells.isEmpty()) {
					if (SelectionUtils.getFirstObject(selection, Plate.class) != null) {
						List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
						for (Plate p : plates) wells.addAll(p.getWells());
					} else if (SelectionUtils.getFirstObject(selection, Compound.class) != null) {
						List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
						for (Compound c: compounds) wells.addAll(c.getWells());
					}
				}
				wellSelectionChanged(wells);
			}
		};
	}

	@Override
	public ISelectionListener initializeHighlightListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != SubWellView.this) {
				List<SubWellSelection> subWellSelections = SubWellUtils.getSubWellSelections(selection);
				if (!subWellSelections.isEmpty()) {
					subWellHighlightChanged(subWellSelections);
				} else {
					List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
					subWellHighlightChanged(new HashSet<>(wells));
				}
			}
		};
	}

	@Override
	public IModelEventListener initializeModelEventListener() {
		return new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				if (currentWells == null) return;
				if (event.type == ModelEventType.ObjectChanged) {
					Object[] items = ModelEventService.getEventItems(event);
					List<Well> wells = SelectionUtils.getAsClass(items, Well.class);
					// Wells have changed: could be classification.
					boolean notifyObservers = false;
					for (Well w : wells) {
						if (currentWells.contains(w)) notifyObservers = true;
					}
					if (notifyObservers) {
						Display.getDefault().asyncExec(() -> getItemSelectionChangedObservable().valueChanged(currentWells));
					}
				}
			}
		};
	}

	@Override
	public BaseLegendView<Well, Well> createLegendView(Composite composite, List<AbstractChartLayer<Well, Well>> layers) {
		ClassificationLegendView<Well, Well> legend = new ClassificationLegendView<Well, Well>(composite, layers, groupingStrategies);
		getItemSelectionChangedObservable().addObserver(legend.getItemSelectionChangedObservable());
		return legend;
	}

	@Override
	public ChartLayerFactory<Well, Well> getChartLayerFactory() {
		if (chartLayerFactory == null) {
			chartLayerFactory = new SubWellChartLayerFactory();
		}
		return chartLayerFactory;
	}

	@Override
	public List<IGroupingStrategy<Well, Well>> getGroupingStrategies() {
		return groupingStrategies;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return subWellPossibleLayers;
	}

	@Override
	protected BaseClassificationSupport<?> createClassificationSupport() {
		return new SubWellClassificationSupport();
	}

	@Override
	protected void addSpecificToolbarButtons(ToolBar toolbar) {
		super.addSpecificToolbarButtons(toolbar);

		// Add plate limits button.
		AbstractChartLayer<Well, Well> chartLayer = getChartView().getBottomEnabledLayer();
		SubWellDataProvider dataProvider = (SubWellDataProvider) chartLayer.getDataProvider();
		plateLimitsButton = new ToolItem(toolbar, SWT.CHECK);
		plateLimitsButton.setToolTipText("Use plate limits");
		plateLimitsButton.setSelection(dataProvider.isUsePlateLimits());
		plateLimitsButton.setImage(IconManager.getIconImage(
				plateLimitsButton.getSelection() ? "limit_plate.png" : "limit_well.png"));
		plateLimitsButton.addListener(SWT.Selection, e -> {
			boolean usePlateLimits = plateLimitsButton.getSelection();
			for (AbstractChartLayer<Well, Well> layer : getChartView().getChartLayers()) {
				if (layer.isDataLayer()) {
					SubWellDataProvider dataprovider = (SubWellDataProvider) layer.getDataProvider();
					dataprovider.setUsePlateLimits(usePlateLimits);
				}
			}
			plateLimitsButton.setImage(IconManager.getIconImage(
					usePlateLimits ? "limit_plate.png" : "limit_well.png"));

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
	protected DropTarget createDropTarget(Control parent) {
		DropTarget dropTarget = new DropTarget(parent, DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getTransfer()});
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				ISelection sel = LocalSelectionTransfer.getTransfer().getSelection();
				// Get selected Features
				List<SubWellFeature> features = SelectionUtils.getObjects(sel, SubWellFeature.class);
				// Get current Features
				List<String> featureNames = getChartView().getSelectedFeatures();
				// Replace current Features with new selected Features starting from 0
				for (int i = 0; i < featureNames.size() && i < features.size(); i++) {
					featureNames.set(i, features.get(i).getDisplayName());
				}
				// Apply the new Feature list to all the data layers
				for (AbstractChartLayer<Well, Well> layer : getChartView().getChartLayers()) {
					if (layer.isDataLayer()) {
						layer.getDataProvider().setSelectedFeatures(featureNames);
					}
				}
				// Redraw the layers
				getChartView().dataChangedForAllLayers();
				getChartView().recalculateDataBounds();
			}
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)
						&& SelectionUtils.getObjects(LocalSelectionTransfer.getTransfer().getSelection(), SubWellFeature.class).size() > 0) {
					event.detail = DND.DROP_LINK;
				}
			}
		});
		return dropTarget;
	}

	@Override
	public void dispose() {
		if (currentJob != null) currentJob.cancel();
		JobUtils.cancelJobs(toString());
		super.dispose();
	}

	@Override
	protected Supplier<Protocol> getProtocolSupplier() {
		return () -> {
			if (currentWells == null || currentWells.isEmpty()) return null;
			return currentWells.get(0).getPlate().getExperiment().getProtocol();
		};
	}

	protected void doLoadView() {
		boolean isUsePlateLimits = false;
		for (AbstractChartLayer<?, ?> layer : getChartView().getChartLayers()) {
			if (!layer.isDataLayer()) continue;
			SubWellDataProvider dataProvider = (SubWellDataProvider) layer.getDataProvider();
			isUsePlateLimits |= dataProvider.isUsePlateLimits();
		}
		if (plateLimitsButton != null) plateLimitsButton.setSelection(isUsePlateLimits);
	}

	private void wellSelectionChanged(List<Well> wells) {
		if (getChartView() == null) return;
		if (wells != null && !wells.isEmpty() && !CollectionUtils.equalsIgnoreOrder(wells, currentWells)) {
			currentWells = wells;
			getItemSelectionChangedObservable().valueChanged(new BitSet());
			getItemSelectionChangedObservable().valueChanged(wells);
		}
	}

	private void subWellHighlightChanged(List<SubWellSelection> subWellSelections) {
		if (getChartView() == null) return;
		if (subWellSelections != null && !subWellSelections.isEmpty()) {
			getItemHighlightChangedObservable().valueChanged(subWellSelections);
		}
	}

	private void subWellHighlightChanged(Set<Well> wells) {
		if (getChartView() == null) return;
		if (wells != null && !wells.isEmpty()) {
			getItemHighlightChangedObservable().valueChanged(wells);
		}
	}

	@Override
	public boolean isSVG() {
		return super.isSVG() && Activator.getDefault().getPreferenceStore().getBoolean(Prefs.EXPORT_SUBWELL_IMAGE_AS_VECTOR);
	}

}