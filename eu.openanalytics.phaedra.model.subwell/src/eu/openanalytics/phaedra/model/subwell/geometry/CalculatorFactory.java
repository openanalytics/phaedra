package eu.openanalytics.phaedra.model.subwell.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class CalculatorFactory {

	private static CalculatorFactory instance;

	private final static String[] REQUIRED_ROLES_BBOX = { "Min X", "Max X", "Min Y", "Max Y" };
	private final static String[] REQUIRED_ROLES_COG  = { "Center X", "Center Y" };
	private final static String[] REQUIRED_ROLES_AREA = { "Area" };
	private final static String[] REQUIRED_ROLES_DIAM = { "Diameter" };

	private IModelEventListener protocolClassListener;

	private CalculatorFactory() {
		// Hidden constructor
		protocolClassListener = event -> {
			if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
				getCache().remove(getCacheKey((ProtocolClass)event.source, null), true);
			}
		};
		ModelEventService.getInstance().addEventListener(protocolClassListener);
	}

	public static CalculatorFactory getInstance() {
		if (instance == null) instance = new CalculatorFactory();
		return instance;
	}

	public Rectangle calculateBounds(Well well, int i) {
		return calculateBounds(well, i, true);
	}

	public int[] calculateCenter(Well well, int i) {
		return calculateCenter(well, i, true);
	}

	public String[] getAvailablePositionRoles() {
		List<String> roles = new ArrayList<>();
		roles.add(""); // The <None> role
		for (String f: REQUIRED_ROLES_BBOX) roles.add(f);
		for (String f: REQUIRED_ROLES_COG) roles.add(f);
		for (String f: REQUIRED_ROLES_AREA) roles.add(f);
		for (String f: REQUIRED_ROLES_DIAM) roles.add(f);
		Collections.sort(roles);
		return roles.toArray(new String[roles.size()]);
	}

	/**
	 * Preload the Subwell Feature data needed to calculate the bounds.
	 * @param wells
	 * @param monitor
	 */
	public void preload(List<Well> wells, IProgressMonitor monitor) {
		if (wells == null || wells.isEmpty()) return;

		ProtocolClass pClass = PlateUtils.getProtocolClass(wells.get(0));

		long[] featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_BBOX);
		long[] additionalFeatureIds = new long[0];
		if (!canCalculate(featureIds)) {
			featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_COG);
			if (!canCalculate(featureIds)) return;

			additionalFeatureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_DIAM);
			if (!canCalculate(additionalFeatureIds)) {
				additionalFeatureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_AREA);
				if (!canCalculate(additionalFeatureIds)) return;
			}
		}

		List<SubWellFeature> features = new ArrayList<>();
		for (long featureId : featureIds) {
			SubWellFeature feature = ProtocolService.getInstance().getSubWellFeature(featureId);
			if (feature != null) features.add(feature);
		}
		for (long featureId : additionalFeatureIds) {
			SubWellFeature feature = ProtocolService.getInstance().getSubWellFeature(featureId);
			if (feature != null) features.add(feature);
		}
		SubWellService.getInstance().preloadData(wells, features, monitor);
	}

	private boolean canCalculate(long[] featureIds) {
		for (long id: featureIds) if (id == 0) return false;
		return true;
	}

	/*
	 * Non-public
	 * **********
	 */

	private ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}
	
	private CacheKey getCacheKey(ProtocolClass pClass, String[] features) {
		return CacheKey.create("CalculatorFactory", pClass, features);
	}
	
	private long[] getRequiredFeatureIds(ProtocolClass pClass, String[] requiredFeatures) {
		CacheKey key = getCacheKey(pClass, requiredFeatures);
		if (getCache().contains(key)) return (long[]) getCache().get(key);

		List<SubWellFeature> features = new ArrayList<>(pClass.getSubWellFeatures());
		Collections.sort(features, ProtocolUtils.FEATURE_NAME_SORTER);

		// Look up the features whose position role matches the required roles.
		long[] ids = new long[requiredFeatures.length];
		boolean noMatches = true;
		for (SubWellFeature f: features) {
			if (f.getPositionRole() == null) continue;
			int index = CollectionUtils.find(requiredFeatures, f.getPositionRole());
			if (index != -1) {
				ids[index] = f.getId();
				noMatches = false;
			}
		}

		// If no matches are found, try to guess them from the feature names.
		if (noMatches) {
			for (int i=0; i<requiredFeatures.length; i++) {
				ids[i] = guessRequiredFeatureId(requiredFeatures[i], features);

				// Hardcoded fallback scenarios
				// Note: some names are too vague to use as fallback: Row, Col, Size, Diam
				if (ids[i] == 0 && requiredFeatures[i].equals("Center X")) ids[i] = guessRequiredFeatureId("CoG X", features);
				if (ids[i] == 0 && requiredFeatures[i].equals("Center Y")) ids[i] = guessRequiredFeatureId("CoG Y", features);
				if (ids[i] == 0 && requiredFeatures[i].equals("Min X")) ids[i] = guessRequiredFeatureId("Minimum X", features);
				if (ids[i] == 0 && requiredFeatures[i].equals("Max X")) ids[i] = guessRequiredFeatureId("Maximum X", features);
				if (ids[i] == 0 && requiredFeatures[i].equals("Min Y")) ids[i] = guessRequiredFeatureId("Minimum Y", features);
				if (ids[i] == 0 && requiredFeatures[i].equals("Max Y")) ids[i] = guessRequiredFeatureId("Maximum Y", features);
			}
		}

		getCache().put(key, ids);
		return ids;
	}

	private long guessRequiredFeatureId(String requiredFeature, List<SubWellFeature> features) {
		String regex = ".*" + requiredFeature.toLowerCase().replace(" ", "[ _]*") + ".*";
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		for (SubWellFeature f: features) {
			if (pattern.matcher(f.getName()).matches()) {
				return f.getId();
			}
		}
		return 0;
	}

	private Rectangle calculateBounds(Well well, int i, boolean allowFallback) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		long[] featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_BBOX);

		boolean canCalculateBounds = true;
		if (featureIds == null) canCalculateBounds = false;
		else for (long id: featureIds) if (id == 0) canCalculateBounds = false;

		if (canCalculateBounds) {
			int xmin = (int)getValue(well, i, featureIds[0]);
			int xmax = (int)getValue(well, i, featureIds[1]);
			int ymin = (int)getValue(well, i, featureIds[2]);
			int ymax = (int)getValue(well, i, featureIds[3]);
			return new Rectangle(xmin, ymin, xmax-xmin, ymax-ymin);
		} else if (allowFallback) {
			int[] center = calculateCenter(well, i, false);
			if (center == null) return null;
			int radius = calculateRadius(well, i);
			if (radius == 0) return null;
			return new Rectangle(center[0]-radius, center[1]-radius, radius*2, radius*2);
		}

		return null;
	}

	private int[] calculateCenter(Well well, int i, boolean allowFallback) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		long[] featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_COG);

		boolean canCalculateCenter = true;
		if (featureIds == null) canCalculateCenter = false;
		else for (long id: featureIds) if (id == 0) canCalculateCenter = false;

		if (canCalculateCenter) {
			int x = (int)getValue(well, i, featureIds[0]);
			int y = (int)getValue(well, i, featureIds[1]);
			return new int[] {x, y};
		} else if (allowFallback) {
			Rectangle bounds = calculateBounds(well, i, false);
			if (bounds == null) return null;
			return new int[] {bounds.x + bounds.width/2, bounds.y + bounds.height/2};
		}

		return null;
	}

	private int calculateRadius(Well well, int i) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);

		long[] featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_DIAM);
		if (featureIds[0] != 0) return (int)(getValue(well, i, featureIds[0]));

		featureIds = getRequiredFeatureIds(pClass, REQUIRED_ROLES_AREA);
		if (featureIds[0] != 0) return (int)(Math.sqrt(getValue(well, i, featureIds[0]))*(1/2.3f));

		return 0;
	}

	private float getValue(Well well, int i, long featureId) {
		SubWellFeature feature = ProtocolService.getInstance().getSubWellFeature(featureId);
		if (feature == null) return Float.NaN;
		float[] data = SubWellService.getInstance().getNumericData(well, feature, 0, false);
		if (data == null || i < 0 || i >= data.length) return Float.NaN;
		return data[i];
	}
}
