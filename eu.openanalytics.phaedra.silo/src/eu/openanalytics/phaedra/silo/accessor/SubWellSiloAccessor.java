package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatapoint;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public class SubWellSiloAccessor extends AbstractSiloAccessor<SubWellItem> {

	public SubWellSiloAccessor(Silo silo) {
		super(silo);
	}

	@Override
	protected List<SubWellItem> loadRowObjects(String datasetName) throws SiloException {
		List<SubWellItem> rows = new ArrayList<>();

		SiloDataset ds = SiloUtils.getDataset(getSilo(), datasetName);
		if (ds == null) return rows;

		SiloDatasetData dsData = getWorkingCopyData(datasetName);
		if (dsData == null) return rows;
		
		SiloDatapoint[] points = dsData.getDataPoints();
		if (points == null || points.length == 0) return rows;

		SiloDatapoint[] distinctWellPoints = Arrays.stream(points).filter(distinctByKey(p -> p.getWellId())).toArray(i -> new SiloDatapoint[i]);
		List<Well> wells = queryWells(distinctWellPoints);

		for (int i = 0; i < points.length; i++) {
			SubWellItem item = new SubWellItem();
			item.setIndex((int) points[i].getSubwellId());
			for (Well well: wells) {
				if (well.getId() == points[i].getWellId()) {
					item.setWell(well);
					break;
				}
			}
			rows.add(item);
		}
		return rows;
	}

	private static Predicate<SiloDatapoint> distinctByKey(Function<? super SiloDatapoint, ?> keyExtractor) {
	    Set<Object> seen = ConcurrentHashMap.newKeySet();
	    return t -> seen.add(keyExtractor.apply(t));
	}
	
	@Override
	protected SiloDatapoint createDataPoint(SubWellItem row) {
		SiloDatapoint dp = new SiloDatapoint();
		dp.setWellId(row.getWell().getId());
		dp.setSubwellId(row.getIndex());
		return dp;
	}

	@Override
	protected void setDefaultValues(SiloDatasetColumn column, SiloDatasetColumnData columnData, List<SubWellItem> newRows) throws SiloException {
		SubWellFeature feature = SiloUtils.getSubWellFeature(column);
		if (feature == null) return;

		// If no rows were specified, update the whole column.
		if (newRows == null) newRows = getRows(columnData.getColumn().getDataset().getName());
		
		for (int i = 0; i < newRows.size(); i++) {
			SubWellItem item = newRows.get(i);
			float[] numericData = feature.isNumeric() ? SubWellService.getInstance().getNumericData(item.getWell(), feature) : null;
			String[] stringData = feature.isNumeric() ? null : SubWellService.getInstance().getStringData(item.getWell(), feature);
			switch (column.getType()) {
			case Float:
				float[] fData = columnData.getFloatData();
				fData[fData.length - newRows.size()] = numericData[item.getIndex()];
				break;
			case Long:
				long[] lData = columnData.getLongData();
				lData[lData.length - newRows.size()] = (long) numericData[item.getIndex()];
			case String:
				String[] sData = columnData.getStringData();
				sData[sData.length - newRows.size()] = stringData[item.getIndex()];
			default:
				// Nothing to set.
			}
		}
	}
}