package eu.openanalytics.phaedra.ui.plate.classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.ClassificationProvider;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.log.ObjectLogService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.table.WellTableColumns;

public class WellClassificationSupport extends BaseClassificationSupport<Well> {
	
	private List<Well> currentWells;

	public WellClassificationSupport() {
		super();
	}

	public WellClassificationSupport(boolean showToolbar, boolean showMenu) {
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
		List<Well> wells = SelectionUtils.getObjects(selection,Well.class);
		if (wells != null && !wells.isEmpty() && !wells.equals(currentWells)) {
			currentWells = wells;
			getCurrentBatch().setItems(currentWells.toArray(new Well[currentWells.size()]));
			refreshNeeded = true;
		}
		if (refreshNeeded) refreshDialog();
	}

	@Override
	protected String calculateCount(FeatureClass featureClass, boolean asPercentage) {
		int count = 0;
		int totalCount = 0;

		List<Plate> allPlates = new ArrayList<>();
		Well[] currentWells = getCurrentBatch().getItems();
		if (currentWells != null) {
			for (Well well: currentWells) {
				CollectionUtils.addUnique(allPlates, well.getPlate());
			}
		}

		for (Plate plate: allPlates) {
			for (Well well: plate.getWells()) {
				totalCount++;
				if (ClassificationService.getInstance().matchesClass(well, featureClass)) count++;
			}
		}

		// Fix for 0/0 when nothing is selected.
		if (count == 0) totalCount = 1;

		return formatCount(count, totalCount, asPercentage);
	}

	@Override
	protected ClassificationProvider[] getClassificationProviders() {
		if (getCurrentProtocolClass() == null) return new ClassificationProvider[0];
		List<Feature> features = ClassificationService.getInstance().findWellClassificationFeatures(getCurrentProtocolClass());
		ClassificationProvider[] providers = new ClassificationProvider[features.size()];
		for (int i=0; i<providers.length; i++) {
			providers[i] = new ClassificationProvider(features.get(i));
		}
		return providers;
	}

	@Override
	protected void doSave(List<ClassificationBatch<Well>> batches, IProgressMonitor monitor) throws IOException {
		monitor.beginTask("Saving well classification", batches.size());
		for (ClassificationBatch<Well> batch: batches) {
			saveBatch(batch, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
	}

	private void saveBatch(ClassificationBatch<Well> batch, IProgressMonitor monitor) throws IOException {

		FeatureClass featureClass = batch.getFeatureClass();
		Well[] data = batch.getItems();
		Feature feature = featureClass.getWellFeature();
		
		List<Plate> platesToRecalc = new ArrayList<>();
		for (Well well: data) {
			CollectionUtils.addUnique(platesToRecalc, well.getPlate());
			checkCanModify(well.getPlate());
		}

		double newNumericValue = ClassificationService.getInstance().getNumericRepresentation(featureClass);
		String newStringValue = ClassificationService.getInstance().getStringRepresentation(featureClass);
		
		for (Plate plate: platesToRecalc) {
			PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
			int wellCount = PlateUtils.getWellCount(plate);
			
			double[] numericValuesToSave = new double[wellCount];
			String[] stringValuesToSave = new String[wellCount];
			
			for (int i=0; i<wellCount; i++) {
				int wellNr = i+1;
				Well modifiedWell = Arrays.stream(data).filter(w -> w.getPlate() == plate && PlateUtils.getWellNr(w) == wellNr).findAny().orElse(null);

				String oldValue = "";
				if (feature.isNumeric()) {
					numericValuesToSave[i] = accessor.getNumericValue(wellNr, feature, null);
					oldValue = "" + numericValuesToSave[i];
					if (modifiedWell != null) numericValuesToSave[i] = newNumericValue;
				} else {
					stringValuesToSave[i] = accessor.getStringValue(wellNr, feature);
					oldValue = "" + stringValuesToSave[i];
					if (modifiedWell != null) stringValuesToSave[i] = newStringValue;
				}
				
				String newValue = feature.isNumeric() ? ""+numericValuesToSave[i] : stringValuesToSave[i];
				ObjectLogService.getInstance().logFeatureChange(modifiedWell, featureClass.getWellFeature().getDisplayName(), oldValue, newValue, featureClass.getLabel());
			}
			
			//TODO This may fail, but at this point feature changes are already written via ObjectLogService.
			if (feature.isNumeric()) PlateService.getInstance().updateWellDataRaw(plate, feature, numericValuesToSave);
			else PlateService.getInstance().updateWellDataRaw(plate, feature, stringValuesToSave);
			
			//TODO This isn't always necessary.
			CalculationService.getInstance().getAccessor(plate).reset();
			CalculationService.getInstance().calculate(plate);
		}
	}

	@Override
	protected ColumnConfiguration[] createItemTableColumns() {
		final DataFormatter dataFormatter = DataTypePrefs.getDefaultDataFormatter();
		return WellTableColumns.configureColumns(false, () -> dataFormatter);
	}
}
