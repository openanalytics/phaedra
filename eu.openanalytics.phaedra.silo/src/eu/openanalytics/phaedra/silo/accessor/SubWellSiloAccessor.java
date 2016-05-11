package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.silo.SiloConstants;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.util.ColumnDescriptor;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SubWellSiloAccessor extends BaseSiloAccessor<SubWellItem> {

	private final static String DS_WELL_ID = SiloConstants.WELL_COL;
	private final static String DS_SUBWELL_INDEX = SiloConstants.INDEX_COL;

	public SubWellSiloAccessor(Silo silo) {
		super(silo);
	}

	@Override
	public String[] getMandatoryColumns() {
		return new String[] { DS_WELL_ID, DS_SUBWELL_INDEX };
	}

	@Override
	protected List<SubWellItem> loadGroup(String dataGroup) throws SiloException {
		List<SubWellItem> rows = new ArrayList<>();

		boolean isNewGroup = !getSiloStructure().getDataGroups().contains(dataGroup);
		if (isNewGroup) return rows;

		long[] wellIds = getWellIds(dataGroup);
		if (wellIds == null) throw new SiloException("Invalid silo format: no " + DS_WELL_ID + " vector found.");
		int[] subWellIds = getSubWellIds(dataGroup);
		if (subWellIds == null) throw new SiloException("Invalid silo format: no " + DS_SUBWELL_INDEX + " vector found.");

		// Obtain a set of unique well ids and retrieve their Well objects.
		Set<Long> uniqueWellIds = new HashSet<>();
		for (long wellId: wellIds) uniqueWellIds.add(wellId);
		long[] uniqueIdArray = new long[uniqueWellIds.size()];
		Iterator<Long> it = uniqueWellIds.iterator();
		for (int i = 0; i < uniqueIdArray.length; i++) uniqueIdArray[i] = it.next();
		List<Well> wells = queryWells(uniqueIdArray);

		for (int i = 0; i < subWellIds.length; i++) {
			SubWellItem item = new SubWellItem();
			for (Well well: wells) {
				if (well.getId() == wellIds[i]) {
					item.setWell(well);
					break;
				}
			}
			item.setIndex(subWellIds[i]);
			rows.add(item);
		}

		return rows;
	}

	@Override
	protected void rowsAdded(String dataGroup, List<SubWellItem> rows) throws SiloException {
		// Update mandatory columns.
		long[] wellIds = getWellIds(dataGroup);
		int[] subWellIds = getSubWellIds(dataGroup);
		if (wellIds == null) wellIds = new long[rows.size()];
		if (subWellIds == null) subWellIds = new int[rows.size()];

		int offset = wellIds.length - rows.size();
		for (int i = offset; i < wellIds.length; i++) {
			wellIds[i] = rows.get(i-offset).getWell().getId();
			subWellIds[i] = rows.get(i-offset).getIndex();
		}

		replaceColumn(dataGroup, DS_WELL_ID, wellIds);
		replaceColumn(dataGroup, DS_SUBWELL_INDEX, subWellIds);
	}

	@Override
	protected void insertDefaultValue(SubWellItem row, Object array, int index, ColumnDescriptor columnDescriptor) throws SiloException {
		SubWellFeature feature = (SubWellFeature)columnDescriptor.feature;
		if (feature.isNumeric()) {
			float[] numericData = SubWellService.getInstance().getNumericData(row.getWell(), feature);
			float numVal = (numericData == null) ? Float.NaN : numericData[row.getIndex()];
			if (columnDescriptor.normalization != null) {
				numVal = (float)NormalizationService.getInstance().getNormalizedValue(row.getWell(), row.getIndex(), feature, columnDescriptor.normalization);
			}

			if (array instanceof float[]) ((float[])array)[index] = numVal;
			else if (array instanceof int[]) ((int[])array)[index] = (int)numVal;
			else if (array instanceof long[]) ((long[])array)[index] = (long)numVal;
			else if (array instanceof double[]) ((double[])array)[index] = numVal;
		} else {
			String[] stringData = SubWellService.getInstance().getStringData(row.getWell(), feature);
			String stringVal = (stringData == null) ? "" : stringData[row.getIndex()];
			if (array instanceof String[]) ((String[])array)[index] = stringVal;
		}
	}

	private long[] getWellIds(String dataGroup) throws SiloException {
		return SiloDataService.getInstance().readLongData(getSilo(), dataGroup, DS_WELL_ID);
	}

	private int[] getSubWellIds(String dataGroup) throws SiloException {
		return SiloDataService.getInstance().readIntData(getSilo(), dataGroup, DS_SUBWELL_INDEX);
	}

}