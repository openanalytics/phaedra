package eu.openanalytics.phaedra.calculation.norm;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizationCache;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * API for performing normalization calculations.
 * <p>
 * Each well feature can have a normalization method set.
 * This method refers to one of the available normalizers.
 * </p>
 * <p>
 * Note that this service is designed primarily for internal use.
 * It is always preferable to call {@link CalculationService#getAccessor(Plate)} instead,
 * and use that accessor to obtain normalized feature values.
 * </p>
 */
public class NormalizationService {

	private static NormalizationService instance = new NormalizationService();

	private NormalizationRegistry normalizationRegistry;
	private NormalizationCache normalizationCache;

	public final static String NORMALIZATION_NONE = "NONE";
	public final static String NORMALIZATION_CUSTOM = "CUSTOM";

	private NormalizationService() {
		// Hidden constructor
		normalizationRegistry = new NormalizationRegistry();
		normalizationCache = new NormalizationCache();
	}

	public static NormalizationService getInstance() {
		return instance;
	}
	
	
	public INormalizer getNormalizer(String id) {
		return normalizationRegistry.getNormalizer(id);
	}
	
	/**
	 * <p>Get the available normalization methods.</p>
	 *
	 * @return An array containing the names of the available normalization methods.
	 */
	public String[] getNormalizations() {
		String[] norms = normalizationRegistry.getNormalizationIds();
		String[] result = new String[norms.length+1];
		System.arraycopy(norms, 0, result, 1, norms.length);
		result[0] = NORMALIZATION_NONE;
		return result;
	}

	/**
	 * <p>Normalize a raw well feature value.</p>
	 *
	 * @param p The plate containing the well
	 * @param f The well feature to normalize
	 * @param norm The normalization method
	 * @param row The well row
	 * @param col The well column
	 * @return The requested normalized value
	 * @throws NormalizationException If the normalization cannot be performed for any reason (e.g. control wells not available)
	 */
	public double getNormalizedValue(Plate p, Feature f, String norm, int row, int col) throws NormalizationException {
		NormalizedGrid grid = normalizationCache.getGrid(p, f, norm);
		if (grid == null) {
			NormalizationKey key = new NormalizationKey(p, f, norm);
			grid = doNormalize(key);
			normalizationCache.putGrid(p, f, norm, grid);
		}
		return grid.getValue(row, col);
	}

	/**
	 * <p>Normalize a raw subwell feature value.</p>
	 *
	 * @param well The well containing the subwell item
	 * @param subWellIndex The index of the subwell item in the well
	 * @param f The subwell feature to normalize
	 * @param norm The normalization method
	 * @return The requested normalized value
	 * @throws NormalizationException If the normalization cannot be performed for any reason (e.g. control wells not available)
	 */
	public double getNormalizedValue(Well well, int subWellIndex, SubWellFeature f, String norm) throws NormalizationException {
		NormalizedGrid grid = normalizationCache.getGrid(well, f, norm);
		if (grid == null) {
			NormalizationKey key = new NormalizationKey(well, f, norm);
			grid = doNormalize(key);
			normalizationCache.putGrid(well, f, norm, grid);
		}
		// The indexes of NormalizedGrid are 1-based.
		return grid.getValue(1, 1+subWellIndex);
	}

	/**
	 * <p>Remove all cached values relating to the plate.</p>
	 *
	 * @param plate The plate to purge.
	 */
	public void purgeCache(Plate plate) {
		normalizationCache.purge(plate);
	}

	public enum NormalizationScope {

		PlateWide("Plate-wide normalization", 0),
		ExperimentWide("Experiment-wide normalization", 1);

		private String label;
		private int id;

		NormalizationScope(String label, int id) {
			this.label = label;
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public int getId() {
			return id;
		}

		public static NormalizationScope getFor(int id) {
			for (NormalizationScope s: NormalizationScope.values()) {
				if (s.getId() == id) return s;
			}
			return null;
		}

		public static NormalizationScope getFor(String label) {
			for (NormalizationScope s: NormalizationScope.values()) {
				if (s.getLabel().equals(label)) return s;
			}
			return null;
		}
	}

	private NormalizedGrid doNormalize(NormalizationKey key) {
		INormalizer normalizer = normalizationRegistry.getNormalizer(key.getNormalization());
		if (normalizer == null) throw new IllegalArgumentException("Unknown normalization: " + key.getNormalization());
		NormalizedGrid grid = normalizer.calculate(key);
		return grid;
	}
}
