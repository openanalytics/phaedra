package eu.openanalytics.phaedra.silo.jep;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.silo.Activator;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;

public class SiloScanner extends BaseScanner<SiloStructure> {

	private final static char VAR_SIGN = '~';
	
	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof SiloStructure;
	}
	
	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, SiloStructure group) {
		try {
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(group.getSilo());
			String dataGroup = group.getFullName();
			String[] columns = accessor.getColumns(dataGroup);
			int columnIndex = 0;
			while (columnIndex < columns.length && !fieldNames[0].equalsIgnoreCase(columns[columnIndex])) {
				columnIndex++;
			}

			SiloDataType dataType = accessor.getDataType(dataGroup, columnIndex);
			switch (dataType) {
			case Double:
				return toVector(accessor.getDoubleValues(dataGroup, columnIndex));
			case Float:
				return toVector(accessor.getFloatValues(dataGroup, columnIndex));
			case Integer:
				return toVector(accessor.getIntValues(dataGroup, columnIndex));
			case Long:
				return toVector(accessor.getLongValues(dataGroup, columnIndex));
			case String:
				return toVector(accessor.getStringValues(dataGroup, columnIndex));
			default:
				return toVector(new float[accessor.getRowCount(dataGroup)]);
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return toVector(new float[0]);
	}

}