package eu.openanalytics.phaedra.silo.jep;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.silo.Activator;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;

public class SiloScanner extends BaseScanner<SiloDataset> {

	private final static char VAR_SIGN = '~';
	
	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof SiloDataset;
	}
	
	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, SiloDataset dataset) {
		try {
			String columnName = fieldNames[0];
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(dataset.getSilo());

			SiloDataType dataType = accessor.getColumnDataType(dataset.getName(), columnName);
			switch (dataType) {
			case Float:
				return toVector(accessor.getFloatValues(dataset.getName(), columnName));
			case Long:
				return toVector(accessor.getLongValues(dataset.getName(), columnName));
			case String:
				return toVector(accessor.getStringValues(dataset.getName(), columnName));
			default:
				return toVector(new float[accessor.getRowCount(dataset.getName())]);
			}
		} catch (SiloException e) {
			EclipseLog.error("", e, Activator.getDefault());
		}
		return toVector(new float[0]);
	}

}