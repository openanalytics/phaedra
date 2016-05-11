package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.silo.SiloConstants;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.util.ColumnDescriptor;
import eu.openanalytics.phaedra.silo.vo.Silo;


public class WellSiloAccessor extends BaseSiloAccessor<Well> {

	private final static String DS_WELL_ID = SiloConstants.WELL_COL;

	public WellSiloAccessor(Silo silo) {
		super(silo);
	}

	@Override
	public String[] getMandatoryColumns() {
		return new String[] { DS_WELL_ID };
	}

	@Override
	protected List<Well> loadGroup(String dataGroup) throws SiloException {
		boolean isNewGroup = !getSiloStructure().getDataGroups().contains(dataGroup);
		if (isNewGroup) return new ArrayList<>();

		long[] wellIds = getWellIds(dataGroup);
		if (wellIds == null) throw new SiloException("Invalid silo format: no " + DS_WELL_ID + " vector found.");

		return queryWells(wellIds);
	}

	@Override
	protected void rowsAdded(String dataGroup, List<Well> rows) throws SiloException {
		// Update mandatory columns.
		long[] wellIds = getWellIds(dataGroup);
		if (wellIds == null) {
			// No data available yet: this is a new group.
			wellIds = new long[rows.size()];
		}
		int offset = wellIds.length - rows.size();
		for (int i = offset; i < wellIds.length; i++) {
			wellIds[i] = rows.get(i-offset).getId();
		}
		SiloDataService.getInstance().replaceData(getSilo(), dataGroup, DS_WELL_ID, wellIds);
	}

	@Override
	protected void insertDefaultValue(Well well, Object array, int index, ColumnDescriptor columnDescriptor) throws SiloException {
		Feature feature = (Feature)columnDescriptor.feature;
		PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
		if (array instanceof float[]) ((float[])array)[index] = (float)dataAccessor.getNumericValue(well, feature, columnDescriptor.normalization);
		else if (array instanceof int[]) ((int[])array)[index] = (int)dataAccessor.getNumericValue(well, feature, columnDescriptor.normalization);
		else if (array instanceof long[]) ((long[])array)[index] = (long)dataAccessor.getNumericValue(well, feature, columnDescriptor.normalization);
		else if (array instanceof double[]) ((double[])array)[index] = dataAccessor.getNumericValue(well, feature, columnDescriptor.normalization);
		else if (array instanceof String[]) ((String[])array)[index] = dataAccessor.getStringValue(well, feature);
	}

	private long[] getWellIds(String dataGroup) throws SiloException {
		return SiloDataService.getInstance().readLongData(getSilo(), dataGroup, DS_WELL_ID);
	}
}