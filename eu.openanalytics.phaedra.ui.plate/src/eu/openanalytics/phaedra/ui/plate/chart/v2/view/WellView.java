package eu.openanalytics.phaedra.ui.plate.chart.v2.view;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
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
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.plate.chart.v2.data.WellDataProvider;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.ClassificationGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.CompConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.CompGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.ConcGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.ExperimentGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.PlateGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellColumnGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellRowGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellStatusGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.chart.v2.grouping.WellTypeGroupingStrategy;
import eu.openanalytics.phaedra.ui.plate.classification.BaseClassificationSupport;
import eu.openanalytics.phaedra.ui.plate.classification.WellClassificationSupport;

public abstract class WellView extends JEPView<Plate, Well> {

	private List<Well> currentWellSel;
	private List<Well> currentWells;
	private WellChartLayerFactory chartLayerFactory;

	private ToolItem defaultNormalizationButton;

	private Job currentJob;

	private List<IGroupingStrategy<Plate, Well>> groupingStrategies = new ArrayList<IGroupingStrategy<Plate, Well>>() {
		private static final long serialVersionUID = -1016582051043014539L;
		{
			add(new DefaultGroupingStrategy<Plate, Well>());
			add(new ExperimentGroupingStrategy());
			add(new PlateGroupingStrategy());
			add(new ClassificationGroupingStrategy());
			add(new WellColumnGroupingStrategy());
			add(new WellRowGroupingStrategy());
			add(new WellStatusGroupingStrategy());
			add(new WellTypeGroupingStrategy());
			add(new CompGroupingStrategy());
			add(new ConcGroupingStrategy());
			add(new CompConcGroupingStrategy());
		}
	};

	private List<ChartName> wellPossibleLayers = new ArrayList<ChartName>() {
		private static final long serialVersionUID = 7189085085824744881L;
		{
			add(ChartName.SCATTER_2D);
			add(ChartName.DENSITY_2D);
			add(ChartName.SCATTER_DENSITY_2D);
			add(ChartName.CONTOUR_2D);
			add(ChartName.WELL_IMAGE);
			add(ChartName.COMPOUND);
		}
	};

	@Override
	public ISelectionListener initializeSelectionListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != WellView.this) {
				if (selection instanceof ConfigurableStructuredSelection) {
					// The selection does not want to use the Wells Plates as input.
					if (((ConfigurableStructuredSelection) selection).hasConfig(ConfigurableStructuredSelection.NO_PARENT)) {
						List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
						if (!wells.isEmpty()) {
							wellSelectionChanged(wells);
							return;
						}
					}
				}
				// Default behavior, use all the wells from the plates.
				List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
				if (plates.isEmpty()) {
					List<Experiment> exps = SelectionUtils.getObjects(selection, Experiment.class);
					exps.forEach(e -> plates.addAll(PlateService.getInstance().getPlates(e)));
				}
				plateSelectionChanged(plates);
			}
		};
	}

	@Override
	public ISelectionListener initializeHighlightListener() {
		return (IWorkbenchPart part, ISelection selection) -> {
			if (part != WellView.this) {
				if (selection instanceof ConfigurableStructuredSelection) {
					if (((ConfigurableStructuredSelection) selection).hasConfig(ConfigurableStructuredSelection.NO_PARENT)) {
						return;
					}
				}
				List<Well> wells = SelectionUtils.getObjects(selection, Well.class);
				// Check for compounds when no wells selection present.
				if (wells.isEmpty()) {
					List<Compound> compounds = SelectionUtils.getObjects(selection, Compound.class);
					for (Compound c : compounds) {
						wells.addAll(c.getWells());
					}
				}
				wellHighlightChanged(wells);
			}
		};
	}

	@Override
	public IModelEventListener initializeModelEventListener() {
		return event -> {
			if (currentWells == null) return;
			if (event.type == ModelEventType.Calculated) {
				if (event.source instanceof Plate) {
					Plate plate = (Plate)event.source;
					for (Well w : currentWells) {
						if (w.getPlate().equals(plate)) {
							getItemSelectionChangedObservable().valueChanged(currentWells);
							return;
						}
					}
				}
			} else if (event.type == ModelEventType.ValidationChanged) {
				Object[] items = ModelEventService.getEventItems(event);
				List<Plate> plates = SelectionUtils.getAsClass(items, Plate.class);
				for (Plate p : plates) {
					for (Well w : currentWells) {
						if (w.getPlate().equals(p)) {
							getItemSelectionChangedObservable().valueChanged(currentWells);
							return;
						}
					}
				}
			}
		};
	}

	@Override
	public BaseLegendView<Plate, Well> createLegendView(Composite composite, List<AbstractChartLayer<Plate, Well>> layers) {
		ClassificationLegendView<Plate, Well> legend = new ClassificationLegendView<Plate, Well>(composite, layers, groupingStrategies);
		getItemSelectionChangedObservable().addObserver(legend.getItemSelectionChangedObservable());
		return legend;
	}

	@Override
	public ChartLayerFactory<Plate, Well> getChartLayerFactory(){
		if (chartLayerFactory == null) {
			chartLayerFactory = new WellChartLayerFactory();
		}
		return chartLayerFactory;
	}

	@Override
	public List<IGroupingStrategy<Plate, Well>> getGroupingStrategies() {
		return groupingStrategies;
	}

	@Override
	public List<ChartName> getPossibleLayers() {
		return wellPossibleLayers;
	}


	@Override
	protected BaseClassificationSupport<?> createClassificationSupport() {
		return new WellClassificationSupport();
	}

	@Override
	protected void addSpecificToolbarButtons(ToolBar toolbar) {
		super.addSpecificToolbarButtons(toolbar);

		// Add plate limits button
		AbstractChartLayer<Plate, Well> chartLayer = getChartView().getBottomEnabledLayer();
		WellDataProvider dataProvider = (WellDataProvider) chartLayer.getDataProvider();
		defaultNormalizationButton = new ToolItem(toolbar, SWT.CHECK);
		defaultNormalizationButton.setToolTipText("Use default normalization (for features only)");
		defaultNormalizationButton.setSelection(dataProvider.isUseDefaultNormalization());
		defaultNormalizationButton.setImage(IconManager.getIconImage(
				defaultNormalizationButton.getSelection() ? "raw_normalized_n.png" : "raw_normalized_r.png"));
		defaultNormalizationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean useDefaultNormalization = defaultNormalizationButton.getSelection();
				for (AbstractChartLayer<Plate, Well> chartLayer : getChartView().getChartLayers()) {
					if (chartLayer.isDataLayer()) {
						WellDataProvider dataProvider = (WellDataProvider) chartLayer.getDataProvider();
						dataProvider.setUseDefaultNormalization(useDefaultNormalization);
					}
				}
				defaultNormalizationButton.setImage(IconManager.getIconImage(
						useDefaultNormalization ? "raw_normalized_n.png" : "raw_normalized_r.png"));

				if (currentWells != null) {
					// Trigger new Plate load so that feature values will be loaded with normalized values
					getItemSelectionChangedObservable().valueChanged(currentWells);
				}
			}
		});
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
				// Get current Features
				List<String> featureNames = getChartView().getSelectedFeatures();
				// Replace current Features with new selected Features starting from 0
				for (int i = 0; i < featureNames.size() && i < features.size(); i++) {
					featureNames.set(i, features.get(i).getDisplayName());
				}
				// Apply the new Feature list to all the data layers
				for (AbstractChartLayer<Plate, Well> layer : getChartView().getChartLayers()) {
					if (layer.isDataLayer()) {
						layer.getDataProvider().setSelectedFeatures(featureNames);
					}
				}
				// Redraw the layers
				getChartView().dataChangedForAllLayers();
				getChartView().recalculateDataBounds();
			}
			@Override
			public void dropAccept(DropTargetEvent event) {
				super.dropAccept(event);
			}
			@Override
			public void dragOver(DropTargetEvent event) {
				super.dragOver(event);
			}
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)
						&& SelectionUtils.getObjects(LocalSelectionTransfer.getTransfer().getSelection(), Feature.class).size() > 0) {
					event.detail = DND.DROP_LINK;
				}
			}
		});
		return dropTarget;
	}

	@Override
	public void dispose() {
		if (currentJob != null) currentJob.cancel();
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
			WellDataProvider chartSettings = (WellDataProvider) layer.getDataProvider();
			isUsePlateLimits |= chartSettings.isUseDefaultNormalization();
		}
		if (defaultNormalizationButton != null) defaultNormalizationButton.setSelection(isUsePlateLimits);
	}

	private void plateSelectionChanged(final List<Plate> plates) {
		List<Well> wells = new ArrayList<>();
		plates.forEach(p -> wells.addAll(p.getWells()));
		wellSelectionChanged(wells);
	}

	private void wellSelectionChanged(final List<Well> wells) {
		if (wells != null && !wells.isEmpty() && !CollectionUtils.equalsIgnoreOrder(wells, currentWells)) {
			currentWells = wells;

			getItemHighlightChangedObservable().valueChanged(new BitSet());
			getItemSelectionChangedObservable().valueChanged(wells);
		}
	}

	private void wellHighlightChanged(List<Well> wells) {
		if (getChartView() == null) return;
		if (wells != null && !wells.isEmpty() && !CollectionUtils.equalsIgnoreOrder(wells, currentWellSel)) {
			currentWellSel = wells;

			getItemHighlightChangedObservable().valueChanged(wells);
		}
	}

	@Override
	public boolean isSVG() {
		return super.isSVG() && Activator.getDefault().getPreferenceStore().getBoolean(Prefs.EXPORT_WELL_IMAGE_AS_VECTOR);
	}

}