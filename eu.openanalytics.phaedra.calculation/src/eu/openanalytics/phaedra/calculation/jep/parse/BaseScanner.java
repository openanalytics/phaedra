package eu.openanalytics.phaedra.calculation.jep.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsmp.djep.vectorJep.values.MVector;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public abstract class BaseScanner<ENTITY> implements IScanner {

	private Map<String, VarReference> refCache;
	private int refCount;

	protected final static String SCOPE_SUBWELL = "subwell";
	protected final static String SCOPE_WELL = "well";
	protected final static String SCOPE_PLATE = "plate";
	protected final static String SCOPE_EXPERIMENT = "exp";
	
	private final static String SCOPE_SEPARATOR = "->";
	private final static String[] SCOPES = { SCOPE_SUBWELL, SCOPE_WELL, SCOPE_PLATE, SCOPE_EXPERIMENT };
	
	public BaseScanner() {
		this.refCache = new HashMap<String, VarReference>();
		this.refCount = 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public VarReference[] scan(JEPExpression expression, Object obj) {

		List<VarReference> refs = new ArrayList<>();

		if (isValidObject(obj)) {
			String expressionToParse = expression.getExpression();
			int offset = 0;
			char varSign = getVarSign();

			while (offset < expressionToParse.length()) {

				int start = expressionToParse.indexOf(varSign, offset);
				if (start < offset) {
					break;
				}

				int end = expressionToParse.indexOf(varSign, start + 1);
				if (end < start) {
					throw new CalculationException("No closing " + varSign + " for: \""
							+ expressionToParse.substring(start, Math.max(expressionToParse.length(), start + 10)) + "\"");
				}

				String refToReplace = expressionToParse.substring(start, end + 1);

				// Use the cache to avoid creating the same ref twice.
				if (!refCache.containsKey(refToReplace)) {
					VarReference ref = createRef(refToReplace, (ENTITY) obj);
					refCache.put(refToReplace, ref);
					if (ref != null) refs.add(ref);
				}

				offset = Math.min(end + 1, expressionToParse.length());
			}
		}

		return refs.toArray(new VarReference[refs.size()]);
	}

	protected VarReference createRef(String substring, ENTITY obj) {
		String fieldName = substring.substring(1, substring.length() - 1);
		String refToReplace = substring;
		String refName = createRefName();
		
		String[] parts = fieldName.split(SCOPE_SEPARATOR);
		String scope = null;
		if (parts.length > 1 && CollectionUtils.find(SCOPES, parts[0]) >= 0) {
			scope = parts[0];
			String[] subParts = new String[parts.length-1];
			System.arraycopy(parts, 1, subParts, 0, subParts.length);
			parts = subParts;
		}
		Object value = getValueForRef(scope, parts, obj);

		VarReference ref = null;
		if (value != null) ref = new VarReference(refToReplace, refName, value);

		return ref;
	}

	protected String createRefName() {
		return "ref" + this.getClass().getSimpleName() + (refCount++);
	}

	protected abstract char getVarSign();

	protected abstract Object getValueForRef(String scope, String[] fieldNames, ENTITY obj);
	
	protected abstract boolean isValidObject(Object obj);

	/*
	 * Utility methods
	 */

	protected List<Well> getWells(Plate plate) {
		// Get wells, sorted by WELLNR.
		List<Well> wells = new ArrayList<>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
		return wells;
	}

	protected List<Plate> getPlates(Experiment exp) {
		// Get plates, sorted by ID.
		List<Plate> plates = new ArrayList<>(PlateService.getInstance().getPlates(exp));
		Collections.sort(plates, PlateUtils.PLATE_ID_SORTER);
		return plates;
	}

	protected MVector toVector(double[] values) {
		MVector valuesVector = new MVector(values.length);
		for(int i = 0; i < values.length;i++){
			valuesVector.setEle(i, values[i]);
		}
		return valuesVector;
	}

	protected MVector toVector(float[] values) {
		MVector valuesVector = new MVector(values.length);
		for(int i = 0; i < values.length;i++){
			valuesVector.setEle(i, values[i]);
		}
		return valuesVector;
	}

	protected MVector toVector(long[] values) {
		MVector valuesVector = new MVector(values.length);
		for(int i = 0; i < values.length;i++){
			valuesVector.setEle(i, values[i]);
		}
		return valuesVector;
	}

	protected MVector toVector(int[] values) {
		MVector valuesVector = new MVector(values.length);
		for(int i = 0; i < values.length;i++){
			valuesVector.setEle(i, values[i]);
		}
		return valuesVector;
	}

	protected MVector toVector(String[] values) {
		MVector valuesVector = new MVector(values.length);
		for(int i = 0; i < values.length;i++){
			valuesVector.setEle(i, values[i]);
		}
		return valuesVector;
	}
}
