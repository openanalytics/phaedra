package eu.openanalytics.phaedra.ui.subwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.ClassificationProvider;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.ui.plate.classification.BaseClassificationSupport;
import eu.openanalytics.phaedra.ui.plate.classification.ClassificationBatch;

public class SubWellClassificationSupport extends BaseClassificationSupport<SubWellItem> {

	public SubWellClassificationSupport() {
		super();
	}

	public SubWellClassificationSupport(boolean showToolbar, boolean showMenu) {
		super(showToolbar, showMenu);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		boolean refreshNeeded = false;
		ProtocolClass pClass = SelectionUtils.getFirstObject(selection, ProtocolClass.class);
		if (pClass != null && !pClass.equals(getCurrentProtocolClass())) {
			setCurrentProtocolClass(pClass);
			refreshNeeded = true;
		}
		List<SubWellSelection> subWellSelections = SelectionUtils.getObjects(selection, SubWellSelection.class);
		if (subWellSelections != null && !subWellSelections.isEmpty()) {
			List<SubWellItem> currentItems = new ArrayList<>();
			for (SubWellSelection sel: subWellSelections) {
				if (sel.getWell() == null) continue;
				BitSet indices = sel.getIndices();
				for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i+1)) {
					currentItems.add(new SubWellItem(sel.getWell(), i));
				}
			}
			getCurrentBatch().setItems(currentItems.toArray(new SubWellItem[currentItems.size()]));
			refreshNeeded = true;
		}
		if (refreshNeeded) refreshDialog();
	}

	@Override
	protected String calculateCount(FeatureClass featureClass, boolean asPercentage) {
		int count = 0;
		int totalCount = 0;

		List<Well> allWells = new ArrayList<>();
		SubWellItem[] currentItems = getCurrentBatch().getItems();
		if (currentItems != null) {
			for (SubWellItem item: currentItems) {
				CollectionUtils.addUnique(allWells, item.getWell());
			}
		}

		for (Well well: allWells) {
			float[] data = SubWellService.getInstance().getNumericData(well, featureClass.getSubWellFeature());
			if (data == null) continue;
			for (int i=0; i<data.length; i++) {
				totalCount++;
				if (ClassificationService.getInstance().matchesClass(well, i, featureClass)) count++;
			}
		}

		// Fix for 0/0 when nothing is selected.
		if (count == 0) totalCount = 1;

		return formatCount(count, totalCount, asPercentage);
	}

	@Override
	protected ClassificationProvider[] getClassificationProviders() {
		if (getCurrentProtocolClass() == null) return new ClassificationProvider[0];
		List<SubWellFeature> features = ClassificationService.getInstance().findSubWellClassificationFeatures(getCurrentProtocolClass());
		ClassificationProvider[] providers = new ClassificationProvider[features.size()];
		for (int i=0; i<providers.length; i++) {
			providers[i] = new ClassificationProvider(features.get(i));
		}
		return providers;
	}

	@Override
	protected void doSave(List<ClassificationBatch<SubWellItem>> batches, IProgressMonitor monitor) throws IOException {
		if (batches.isEmpty()) return;
		monitor.beginTask("Saving subwell classification", batches.size()*3);

		// Assumption: a batch does not contain wells from different plates.

		// Collect all data to save in a Map.
		Map<Plate, Map<Well, Object>> dataMap = new HashMap<>();

		// Initialize the Maps (per plate) and perform permission checks on those plates.
		List<Well> affectedWells = new ArrayList<>();
		for (ClassificationBatch<SubWellItem> batch: batches) {
			SubWellItem[] data = batch.getItems();
			if (data.length == 0) continue;
			CollectionUtils.addUnique(affectedWells, data[0].getWell());
			for (int i = 0; i < data.length; i++) {
				Plate p = data[i].getWell().getPlate();
				checkCanModify(p);
				if (dataMap.get(p) == null) dataMap.put(p, new HashMap<>());
			}
		}

		// Transfer the data from the batches into the Maps.
		SubWellFeature feature = null;
		for (ClassificationBatch<SubWellItem> batch: batches) {
			if (batch.getItems().length == 0) continue;
			Plate plate = batch.getItems()[0].getWell().getPlate();
			feature = batch.getFeatureClass().getSubWellFeature();
			prepareBatch(batch, dataMap.get(plate), new SubProgressMonitor(monitor, 1));
		}
		
		// Then update the subwell data (in one transaction per plate).
		for (Plate plate: dataMap.keySet()) {
			SubWellService.getInstance().updateData(dataMap.get(plate), feature);
			monitor.worked(1);
			CalculationService.getInstance().triggerSubWellCalculation(plate);
			monitor.worked(1);
		}

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				ModelEvent event = new ModelEvent(affectedWells, ModelEventType.ObjectChanged, 0);
				ModelEventService.getInstance().fireEvent(event);
			}
		});
		monitor.done();
	}

	private void prepareBatch(ClassificationBatch<SubWellItem> batch, Map<Well, Object> dataMap, IProgressMonitor monitor) throws IOException {

		FeatureClass featureClass = batch.getFeatureClass();
		SubWellItem[] data = batch.getItems();
		SubWellFeature feature = featureClass.getSubWellFeature();

		monitor.beginTask("Preparing classification data for " + featureClass.getLabel(), data.length);

		float newNumericValue = ClassificationService.getInstance().getNumericRepresentation(featureClass);
		String newStringValue = ClassificationService.getInstance().getStringRepresentation(featureClass);

		// Build updated lists of data.
		Well well = null;
		for (int i = 0; i < data.length; i++) {
			well = data[i].getWell();

			if (feature.isNumeric()) {
				float[] values = (float[]) dataMap.get(well);
				if (values == null) {
					// Not yet cached: retrieve data from service.
					values = SubWellService.getInstance().getNumericData(well, feature);
					dataMap.put(well, values);
				}
				if (values == null || values.length == 0) {
					values = new float[getCellCount(well)];
					dataMap.put(well, values);
				}
				values[data[i].getIndex()] = newNumericValue;
			} else {
				String[] values = (String[]) dataMap.get(well);
				if (values == null) {
					// Not yet cached: retrieve data from service.
					values = SubWellService.getInstance().getStringData(well, feature);
					dataMap.put(well, values);
				}
				if (values == null || values.length == 0) {
					values = new String[getCellCount(well)];
					dataMap.put(well, values);
				}
				values[data[i].getIndex()] = newStringValue;
			}
			monitor.worked(1);
		}

		monitor.done();
	}

	private int getCellCount(Well well) {
		int entityCount = SubWellService.getInstance().getNumberOfCells(well);
		if (entityCount == 0) {
			SubWellFeature sampleFeature = SubWellService.getInstance().getSampleFeature(well);
			Object sampleData = SubWellService.getInstance().getAnyData(well, sampleFeature);
			entityCount = (sampleData instanceof float[]) ? ((float[])sampleData).length : ((String[])sampleData).length;
		}
		if (entityCount == 0) {
			EclipseLog.error("Cannot determine cell count for well " + well + ", plate " + well.getPlate(), null, Activator.getDefault());
		}
		return entityCount;
	}
	
	@Override
	protected ColumnConfiguration[] createItemTableColumns() {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();

		ColumnConfiguration config = ColumnConfigFactory.create("Well", ColumnDataType.String, 50);
		RichLabelProvider lp = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				SubWellItem item = (SubWellItem)element;
				return NumberUtils.getWellCoordinate(item.getWell().getRow(), item.getWell().getColumn());
			}
		};
		config.setLabelProvider(lp);
		configs.add(config);

		config = ColumnConfigFactory.create("Subwell Item", ColumnDataType.String, 80);
		lp = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				SubWellItem item = (SubWellItem)element;
				return "" + item.getIndex();
			}
		};
		config.setLabelProvider(lp);
		config.setSorter(new Comparator<SubWellItem>() {
			@Override
			public int compare(SubWellItem i1, SubWellItem i2) {
				if (i1 == null && i2 == null) return 0;
				if (i1 == null) return -1;
				if (i2 == null) return 1;
				return i1.getIndex() - i2.getIndex();
			}
		});
		configs.add(config);

		if (getCurrentProtocolClass() != null) {
			final List<SubWellFeature> features = new ArrayList<>(getCurrentProtocolClass().getSubWellFeatures());
			Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

			for (int i=0; i<features.size(); i++) {
				final SubWellFeature feature = features.get(i);

				ColumnDataType type = feature.isNumeric() ? ColumnDataType.Numeric : ColumnDataType.String;
				config = ColumnConfigFactory.create(feature.getName(), type, 100);

				RichLabelProvider labelProvider = new RichLabelProvider(config){
					@Override
					public String getText(Object element) {
						if (features == null || features.isEmpty()) return "";
						SubWellItem item = (SubWellItem)element;
						if (feature.isNumeric()) {
							float[] data = SubWellService.getInstance().getNumericData(item.getWell(), feature);
							if (data == null || data.length <= item.getIndex()) return "";
							return Formatters.getInstance().format(data[item.getIndex()], feature);
						} else {
							String[] data = SubWellService.getInstance().getStringData(item.getWell(), feature);
							if (data == null || data.length <= item.getIndex()) return "";
							return data[item.getIndex()];
						}
					}
				};
				config.setLabelProvider(labelProvider);

				config.setSorter(new Comparator<SubWellItem>() {
					@Override
					public int compare(SubWellItem i1, SubWellItem i2) {
						if (i1 == null && i2 == null) return 0;
						if (i1 == null) return -1;
						if (i2 == null) return 1;
						if (feature.isNumeric()) {
							float v1 = 0;
							float v2 = 0;
							float[] data = SubWellService.getInstance().getNumericData(i1.getWell(), feature);
							if (data != null && data.length > i1.getIndex()) v1 = data[i1.getIndex()];
							data = SubWellService.getInstance().getNumericData(i2.getWell(), feature);
							if (data != null && data.length > i2.getIndex()) v2 = data[i2.getIndex()];
							if (v1 > v2) return 1;
							if (v1 == v2) return 0;
							return -1;
						} else {
							String v1 = null;
							String v2 = null;
							String[] data = SubWellService.getInstance().getStringData(i1.getWell(), feature);
							if (data != null && data.length > i1.getIndex()) v1 = data[i1.getIndex()];
							data = SubWellService.getInstance().getStringData(i2.getWell(), feature);
							if (data != null && data.length > i2.getIndex()) v2 = data[i2.getIndex()];
							if (v1 == null) return 1;
							return v1.compareTo(v2);
						}
					}
				});
				config.setTooltip(feature.getName());
				configs.add(config);
			}
		}

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
