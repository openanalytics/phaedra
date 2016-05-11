package eu.openanalytics.phaedra.calculation.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.stat.cache.StatCache;
import eu.openanalytics.phaedra.calculation.stat.cache.StatContainer;
import eu.openanalytics.phaedra.calculation.stat.ctx.ArgumentStatContext;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;
import eu.openanalytics.phaedra.calculation.stat.ctx.SimpleStatContext;
import eu.openanalytics.phaedra.calculation.stat.ctx.StatContextFactory;
import eu.openanalytics.phaedra.calculation.stat.filter.IFilter;
import eu.openanalytics.phaedra.calculation.stat.filter.NaNFilter;
import eu.openanalytics.phaedra.calculation.stat.persist.PersistentPlateStats;
import eu.openanalytics.phaedra.calculation.stat.persist.PersistentStatAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * Entry point for statistic computations on plate data. Statistics are always
 * given per plate per feature, but may also be given per plate per feature per
 * welltype.
 *
 * Results are cached for future requests.
 */
public class StatService {

	private static StatService instance = new StatService();

	private StatCalculatorRegistry calculatorRegistry;
	private StatCache statCache;
	
	private PersistentStatAccessor persistentStatAccessor;
	private Set<Plate> persistentStatsDisabled;
	private Set<Plate> persistentStatsLoaded;
	
	private List<IFilter> statFilters;

	private StatService() {
		// Hidden constructor
		calculatorRegistry = new StatCalculatorRegistry();
		statCache = new StatCache();
		
		persistentStatAccessor = new PersistentStatAccessor();
		persistentStatsDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());
		persistentStatsLoaded = Collections.newSetFromMap(new ConcurrentHashMap<>());
		
		// Fitlers that are applied to any stat calculation.
		statFilters = new ArrayList<>();
		statFilters.add(new NaNFilter());
	}

	public static StatService getInstance() {
		return instance;
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public String[] getAvailableStats() {
		return calculatorRegistry.getCalculatorNames();
	}

	public double calculate(String stat, double[] values) {
		return doCalculate(stat, new SimpleStatContext(values));
	}

	public double calculate(String stat, double[] values, Object... args) {
		return doCalculate(stat, new ArgumentStatContext(values, args));
	}

	public double calculate(String stat, Plate p, Feature f, String wellType, String norm) {
		return calculate(new StatQuery(stat, p, f, wellType, norm));
	}

	public double calculate(String stat, Experiment exp, Feature f, String wellType, String norm) {
		return calculate(new StatQuery(stat, exp, f, wellType, norm));
	}

	public double calculate(String stat, Well w, SubWellFeature f) {
		return calculate(new StatQuery(stat, w, f, null, null));
	}

	public double calculate(String stat, Plate p, SubWellFeature f, String wellType) {
		return calculate(new StatQuery(stat, p, f, wellType, null));
	}

	public double calculate(StatQuery query) {

		if (query == null || query.getStat() == null || query.getFeature() == null) return Double.NaN;

		String stat = query.getStat();

		// Replace normalization NONE with null, otherwise cache cannot match.
		String norm = query.getNormalization();
		if (norm == null || norm.equals("NONE")) query.setNormalization(null);

		// For plate stat queries, make sure persistent (quick-access) stats are loaded.
		if (query.getObject() instanceof Plate) loadPersistentPlateStats((Plate)query.getObject());
		
		StatContainer stats = statCache.getStats(query);

		// Stat exists in a cached set: return the cached value.
		if (stats != null && stats.contains(stat)) {
			return stats.get(stat);
		}

		// Stat not found in cache. Calculate and add to set.
		if (stats == null) {
			stats = new StatContainer();
			statCache.add(query, stats);
		}

		double value = doCalculate(query);
		stats.add(stat, value);
		return stats.get(stat);
	}

	public void purgeCache(Plate p) {
		// Remove all stored values relating to the plate.
		statCache.remove(p);
		persistentStatsLoaded.remove(p);
	}

	public void loadPersistentPlateStats(Plate p) {
		if (persistentStatsDisabled.contains(p)) return;
		if (persistentStatsLoaded.contains(p)) return;
		synchronized (p) {
			if (persistentStatsLoaded.contains(p)) return;
			doLoadPersistentPlateStats(p);
			persistentStatsLoaded.add(p);
		}
	}

	public void togglePersistentStatsEnabled(Plate p, boolean enabled) {
		if (enabled) persistentStatsDisabled.remove(p);
		else persistentStatsDisabled.add(p);
	}

	public void updatePersistentPlateStats(Plate p) {
		// Calculate and store plate stats.
		doUpdatePersistentPlateStats(p);
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private double doCalculate(StatQuery query) {
		// If the feature is not numeric, do not bother trying to calculate.
		IValueObject f = query.getFeature();
		if (f instanceof Feature && !((Feature)f).isNumeric()) return Double.NaN;
		if (f instanceof SubWellFeature && !((SubWellFeature)f).isNumeric()) return Double.NaN;

		IStatContext ctx = StatContextFactory.createContext(query);
		double retVal = doCalculate(query.getStat(), ctx);
		return retVal;
	}

	private double doCalculate(String stat, IStatContext ctx) {
		IStatCalculator calc = calculatorRegistry.getCalculator(stat);
		if (calc == null) throw new IllegalArgumentException("Unknown statistic: " + stat);
		for (IFilter filter: statFilters) ctx.applyFilter(filter);
		return calc.calculate(ctx);
	}

	private void doLoadPersistentPlateStats(Plate plate) {
		// Persistent plate stats are a quick way to get plate stats
		// without having to load & calculate feature values.
		PersistentPlateStats persistentStats = persistentStatAccessor.getStoredStats(plate);
		if (persistentStats == null) return;

		// Note: all saved stats are against normalization NONE.

		List<Feature> features = PlateUtils.getFeatures(plate);
		String[] wellTypes = persistentStats.getWellTypes();

		// Load all feature stats
		for (Feature f : features) {
			if (!f.isNumeric()) continue;
			StatContainer container = persistentStats.getStats(f);
			if (container == null) continue;
			StatQuery query = new StatQuery(null, plate, f, null, null);
			statCache.add(query, container);
		}

		// Load all control stats
		for (Feature f : features) {
			if (!f.isNumeric()) continue;

			for (String wellType : wellTypes) {
				StatContainer container = persistentStats.getStats(f, wellType);
				if (container == null) continue;

				StatQuery query = new StatQuery(null, plate, f, wellType, null);
				if (wellType != null && wellType.equalsIgnoreCase("ALL")) {
					query.setWellType(null);
					// Merge with existing stats
					StatContainer existingContainer = statCache.getStats(query);
					if (existingContainer != null) existingContainer.copyTo(container);
				}

				statCache.add(query, container);
			}
		}
	}

	private void doUpdatePersistentPlateStats(Plate plate) {
		PersistentPlateStats stats = new PersistentPlateStats();

		List<Feature> features = PlateUtils.getFeatures(plate);
		List<String> wellTypes = PlateUtils.getWellTypes(plate);

		// Since we'll be needing them, set the accessor to load all features at once.
		CalculationService.getInstance().getAccessor(plate).loadEager(null);

		// Note: all saved stats are against normalization NONE.

		// Calculate all feature stats
		for (Feature f : features) {
			if (!f.isNumeric()) continue;

			StatContainer container = stats.getStats(f);
			if (container == null) {
				container = new StatContainer();
				stats.setStats(f, container);
			}
			for (String stat : PersistentPlateStats.FEATURE_STATS) {
				// Always against the current feature normalization.
				StatQuery query = new StatQuery(stat, plate, f, null, null);
				double value = doCalculate(query);
				container.add(stat, value);
			}
			StatQuery query = new StatQuery(null, plate, f, null, null);
			statCache.add(query, container);
		}

		// Calculate all control stats
		for (Feature f : features) {
			if (!f.isNumeric())
				continue;

			for (String wellType : wellTypes) {
				StatContainer container = stats.getStats(f, wellType);
				if (container == null) {
					container = new StatContainer();
					stats.setStats(f, wellType, container);
				}
				for (String stat : PersistentPlateStats.CONTROL_STATS) {
					// Aways against Raw values.
					StatQuery query = new StatQuery(stat, plate, f, wellType, null);
					double value = doCalculate(query);
					container.add(stat, value);
				}
				StatQuery query = new StatQuery(null, plate, f, wellType, null);
				statCache.add(query, container);
			}
		}

		persistentStatAccessor.saveStoredStats(plate, stats);
		
		//TODO Add to cache
	}
}
