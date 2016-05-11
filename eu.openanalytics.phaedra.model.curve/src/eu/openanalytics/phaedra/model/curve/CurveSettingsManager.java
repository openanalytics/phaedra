package eu.openanalytics.phaedra.model.curve;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.model.curve.dao.CustomSettingsDAO;
import eu.openanalytics.phaedra.model.curve.dao.CustomSettingsDAO.CustomCurveSettings;
import eu.openanalytics.phaedra.model.curve.util.CurveSettingsMapper;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * Convenience class for accessing curve settings with the following advantages:
 * <ul>
 * <li>If a curve has custom settings, those will be returned rather than the feature's default settings</li>
 * <li>Custom settings are cached for better performance</li>
 * <li>Custom settings that are not cached are always queried in batch (per plate) for better performance</li>
 * </ul>
 */
public class CurveSettingsManager {

	private CustomSettingsDAO customSettingsDAO;
	private Set<Plate> loadedPlates;
	
	public CurveSettingsManager() {
		customSettingsDAO = new CustomSettingsDAO(Screening.getEnvironment().getEntityManager());
		loadedPlates = new HashSet<>();
	}
	
	public CurveSettings getSettings(Curve curve, Feature feature) {
		if (curve == null) return CurveSettingsMapper.toSettings(feature);
		
		// Bulk load the plate(s) if they aren't already loaded.
		for (Compound c: curve.getCompounds()) {
			if (!loadedPlates.contains(c.getPlate())) loadSettings(c.getPlate());
		}
		// Look for custom settings, or use default settings.
		CacheKey key = createKey(curve.getId());
		CurveSettings settings = null;
		if (getCache().contains(key)) {
			settings = (CurveSettings) getCache().get(key);
		} else {
			settings = CurveSettingsMapper.toSettings(curve.getFeature());
		}
		return settings;
	}
	
	public void updateSettings(Curve curve, CurveSettings settings) {
		if (settings != null) {
			// If new settings are identical to the defaults, treat it as a reset.
			CurveSettings defaults = CurveSettingsMapper.toSettings(curve.getFeature());
			if (defaults.equals(settings)) settings = null;
		}
		
		customSettingsDAO.clearSettings(curve.getId());
		CacheKey key = createKey(curve.getId());
		if (settings == null) {
			getCache().remove(key);
		} else {
			customSettingsDAO.saveSettings(curve.getId(), settings);
			getCache().put(key, settings);
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void loadSettings(Plate plate) {
		CustomCurveSettings[] plateSettings = customSettingsDAO.loadSettings(plate.getId());
		for (CustomCurveSettings csettings: plateSettings) {
			CacheKey key = createKey(csettings.curveId);
			getCache().put(key, csettings.settings);
		}
		loadedPlates.add(plate);
	}
	
	private ICache getCache() {
		return CacheService.getInstance().getDefaultCache();
	}
	
	private CacheKey createKey(long curveId) {
		return CacheKey.create("CurveSettings", curveId);
	}
}
