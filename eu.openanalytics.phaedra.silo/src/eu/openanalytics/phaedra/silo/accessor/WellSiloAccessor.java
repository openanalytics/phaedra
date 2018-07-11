package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatapoint;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;


public class WellSiloAccessor extends AbstractSiloAccessor<Well> {

	public WellSiloAccessor(Silo silo) {
		super(silo);
	}

	@Override
	protected List<Well> loadRowObjects(String datasetName) throws SiloException {
		List<Well> rows = new ArrayList<>();
		
		SiloDataset ds = SiloUtils.getDataset(getSilo(), datasetName);
		if (ds == null) return rows;

		SiloDatasetData dsData = getWorkingCopyData(datasetName);
		if (dsData == null) return rows;
		
		SiloDatapoint[] points = dsData.getDataPoints();
		if (points == null || points.length == 0) return rows;
		
		return queryWells(points);
	}

	@Override
	protected SiloDatapoint createDataPoint(Well row) {
		SiloDatapoint dp = new SiloDatapoint();
		dp.setWellId(row.getId());
		return dp;
	}

	@Override
	protected void setDefaultValues(SiloDatasetColumn column, SiloDatasetColumnData columnData, List<Well> newRows) throws SiloException {
		Feature feature = SiloUtils.getWellFeature(column);
		if (feature == null) return;

		// If no rows were specified, update the whole column.
		if (newRows == null) newRows = getRows(columnData.getColumn().getDataset().getName());
		
		for (int i = 0; i < newRows.size(); i++) {
			Well well = newRows.get(i);
			//TODO support normalized values
			String normalization = null;
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			switch (column.getType()) {
			case Float:
				float[] fData = columnData.getFloatData();
				fData[fData.length - newRows.size()] = (float) dataAccessor.getNumericValue(well, feature, normalization);
				break;
			case Long:
				long[] lData = columnData.getLongData();
				lData[lData.length - newRows.size()] = (long) dataAccessor.getNumericValue(well, feature, normalization);
			case String:
				String[] sData = columnData.getStringData();
				sData[sData.length - newRows.size()] = dataAccessor.getStringValue(well, feature);
			default:
				// Nothing to set.
			}
		}
	}
}