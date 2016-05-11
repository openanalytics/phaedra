package eu.openanalytics.phaedra.model.curve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.cache.CacheConfig;
import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.stat.StatQuery;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.calculation.stat.ctx.StatContextFactory;
import eu.openanalytics.phaedra.model.curve.dao.CurveDAO;
import eu.openanalytics.phaedra.model.curve.fit.CurveDataPoints;
import eu.openanalytics.phaedra.model.curve.fit.CurveFitException;
import eu.openanalytics.phaedra.model.curve.fit.ICurveFitter;
import eu.openanalytics.phaedra.model.curve.util.CurveFactory;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

/**
 * This service allows interaction with dose-response curves.
 * More precisely, it provides methods for:
 * <ul>
 * <li>Retrieving dose-response curves</li>
 * <li>Fitting dose-response curves</li>
 * <li>Saving manual fit settings</li>
 * </ul>
 */
public class CurveService extends BaseJPAService {

	private static CurveService instance = new CurveService();
	
	public static final int MIN_SAMPLES_FOR_FIT = 4;

	private static final CurveGrouping NO_GROUPING = new CurveGrouping(null, null);
	
	private CurveSettingsManager curveSettingsMgr;
	private CurveDAO curveDAO;
	
	private ICache curveCache;
	private ICache curveImageCache;
	private ICache curveIdCache;

	private CurveService() {
		// Hidden constructor
		curveSettingsMgr = new CurveSettingsManager();
		// Disk caching disabled here because curves cannot be efficiently serialized (feature & compound references).
		curveCache = CacheService.getInstance().createCache(new CacheConfig("CurveDataCache", false));
		curveImageCache = CacheService.getInstance().createCache("CurveImageCache");
		// A helper cache to access curves by their unique ID.
		curveIdCache = CacheService.getInstance().createCache("CurveIdCache");
		curveDAO = new CurveDAO(getEntityManager());
	}

	public static CurveService getInstance() {
		return instance;
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public ImageData getCurveImage(Curve curve, int w, int h) {
		Compound compound = getAnyCompound(curve);
		Feature feature = curve.getFeature();
		CurveGrouping grouping = getGrouping(curve);
		
		CacheKey key = getImageCacheKey(compound, feature, grouping, w, h);
		if (curveImageCache.contains(key)) return (ImageData) curveImageCache.get(key);

		ImageData image = null;
		synchronized (curve) {
			// Check again, in case this thread was waiting for another thread to generate the image.
			if (curveImageCache.contains(key)) return (ImageData) curveImageCache.get(key);
			
			if (curve != null && curve.getPlot() != null) {
				Image img = null;
				try {
					img = PDFToImageConverter.convert(curve.getPlot(), w, h);
					img = ImageUtils.addTransparency(img, 0xFFFFFF);
					image = img.getImageData();
				} catch (IOException e) {
					EclipseLog.error("Failed to render curve: " + e.getMessage(), e, Activator.getDefault());
				} finally {
					if (img != null) img.dispose();
				}
			}
			curveImageCache.put(key, image);
		}
		return image;
	}
	
	public Curve getCurve(long curveId) {
		if (curveIdCache.contains(curveId)) {
			CacheKey key = (CacheKey) curveIdCache.get(curveId);
			if (key == null) return null;
			return (Curve) curveCache.get(key);
		} else {
			Curve curve = curveDAO.getCurve(curveId);
			if (curve != null) addToCache(curve);
			else curveIdCache.put(curveId, null);
			return curve;
		}
	}
	
	public Curve getCurve(Curve curve, Feature feature) {
		return getCurve(curve.getCompounds().get(0), feature, getGrouping(curve), false);
	}
	
	public Curve getCurve(Well well, Feature feature) {
		return getCurve(well, feature, false);
	}
	
	public Curve getCurve(Well well, Feature feature, boolean batchMode) {
		return getCurve(well.getCompound(), feature, getGrouping(well, feature), batchMode);
	}
	
	public Curve getCurve(Compound compound, Feature feature, CurveGrouping grouping, boolean batchMode) {
		// If the feature is incompatible, abort now.
		if (compound == null || feature == null || !PlateUtils.isSameProtocolClass(compound, feature)) {
			return null;
		}
		
		if (grouping == null) grouping = NO_GROUPING;
		CacheKey key = getCacheKey(compound, feature, grouping);

		// First look in the cache.
		if (curveCache.contains(key)) return (Curve) curveCache.get(key);
		synchronized (this) {
			if (curveCache.contains(key)) return (Curve) curveCache.get(key);
			
			// Cache miss, then look in the database.
			Curve curve = null;
			if (batchMode) {
				// Fill cache with nulls, to deal with single-dose compounds which are not returned by the batch query below.
				List<Feature> curveFeatures = CollectionUtils.findAll(PlateUtils.getFeatures(compound), ProtocolUtils.FEATURES_WITH_CURVES);
				Set<CurveGrouping> groupings = new HashSet<>();
				for (Compound c: compound.getPlate().getCompounds()) {
					for (Feature f: curveFeatures) {
						for (CurveGrouping cg: getGroupings(c, f)) groupings.add(cg);
					}
				}
				for (Compound c: compound.getPlate().getCompounds()) {
					for (Feature f: curveFeatures) {
						for (CurveGrouping cg: groupings) addToCache(c, f, cg, null);
					}
				}
				// Batch mode: expect more getCurve calls, so load the entire plate in batch.
				List<Curve> curves = curveDAO.getCurveBatch(compound.getPlate());
				for (Curve c: curves) {
					CurveGrouping cGrouping = getGrouping(c);
					for (Compound comp: c.getCompounds()) addToCache(comp, c.getFeature(), cGrouping, c);
					if (c.getFeature() == feature && c.getCompounds().contains(compound) && grouping.equals(cGrouping)) curve = c;
				}
			} else {
				curve = curveDAO.getCurve(feature, compound, grouping);
			}

			addToCache(compound, feature, grouping, curve);
			return curve;
		}
	}

	public List<Plate> getPlatesWithCurves(List<Experiment> experiments) {
		String[] experimentIds = experiments.stream().map(e -> ""+e.getId()).toArray(len -> new String[len]);
		String alias = JDBCUtils.isOracle() ? "" : " as samples_per_cmp";
		String query =
				"select distinct plate_id from ("
						+ " select c.plate_id, c.platecompound_id, count(w.well_id) as samples"
						+ " from phaedra.hca_plate_well w, phaedra.hca_plate_compound c, phaedra.hca_plate p"
						+ " where w.platecompound_id = c.platecompound_id and c.plate_id = p.plate_id"
						+ " and p.experiment_id in (" + StringUtils.createSeparatedString(experimentIds, ",") + ")"
						+ " group by c.platecompound_id, c.plate_id"
						+ ")" + alias + " where samples >= " + MIN_SAMPLES_FOR_FIT;
		List<?> plateIds = JDBCUtils.queryWithLock(getEntityManager().createNativeQuery(query), getEntityManager());
		if (plateIds.isEmpty()) return new ArrayList<>();
		return getList("select p from Plate p where p.id in ?1", Plate.class, (Object)plateIds);
	}
	
	public void updateCurveSettings(Curve curve, CurveSettings settings) throws CurveFitException {
		for (Compound c: curve.getCompounds()) {
			if (PlateApprovalStatus.APPROVED.matches(c.getPlate())) throw new CurveFitException("Cannot change settings: plate is approved");
		}
		curveSettingsMgr.updateSettings(curve, settings);
	}

	public CurveSettings getCurveSettings(Curve curve) {
		return curveSettingsMgr.getSettings(curve, curve.getFeature());
	}

	public void fitAllCurves(Plate plate) {
		
		List<Feature> features = CollectionUtils.findAll(ProtocolUtils.getFeatures(plate), ProtocolUtils.FEATURES_WITH_CURVES);
		List<Compound> compounds = streamableList(plate.getCompounds());
		if (features.isEmpty() || compounds.isEmpty()) return;
		
		EclipseLog.info("Fitting curves for " + plate, Activator.getDefault());
		
		List<Object[]> curvesToFit = new ArrayList<>();
		for (Feature feature: features) {
			for (Compound compound: compounds) {
				curvesToFit.add(new Object[] { compound, feature });
			}
		}
		
		Consumer<Object[]> fitter = o -> {
			try {
				fitAllCurves((Compound) o[0], (Feature) o[1]);
			} catch (CurveFitException e) {
				EclipseLog.error("Curve fit failed for " + o[0] + " @ " + o[1] + ": " + e.getMessage(), null, Activator.getDefault());
			}
		};

		// When launched from main thread, this can deadlock, because R nodes do a Display.syncExec when starting up.
		if (Thread.currentThread() == Display.getDefault().getThread()) {
			EclipseLog.warn("Batch curve fitting (fitAllCurves) called from main thread. Fitting single-threaded to avoid UI deadlock.", Activator.getDefault());
			curvesToFit.stream().forEach(fitter);
		} else {
			curvesToFit.parallelStream().forEach(fitter);
		}
	}
	
	public void fitAllCurves(Compound compound, Feature feature) throws CurveFitException {
		Assert.isLegal(compound != null);
		Assert.isLegal(feature != null);
		
		CurveGrouping[] groupings = getGroupings(compound, feature);
		for (CurveGrouping grouping: groupings) {
			fitCurve(compound, feature, grouping);
		}
	}
	
	public void fitCurve(Curve curve) throws CurveFitException {
		fitCurve(getAnyCompound(curve), curve.getFeature(), getGrouping(curve));
	}
	
	public Curve fitCurve(Compound compound, Feature feature, CurveGrouping grouping) throws CurveFitException {
		Assert.isLegal(compound != null);
		Assert.isLegal(feature != null);
		
		List<Compound> compounds = CalculationService.getInstance().getMultiploCompounds(compound);
		if (compounds.isEmpty()) throw new CurveFitException("No compound(s) to fit");
		for (Compound c: compounds) {
			if (PlateValidationStatus.INVALIDATED.matches(c.getPlate())) continue;
			if (!PlateCalcStatus.CALCULATION_OK.matches(c.getPlate())) throw new CurveFitException(c.getPlate() + ": plate calculation is not OK");
			if (PlateApprovalStatus.APPROVED.matches(c.getPlate())) throw new CurveFitException(c.getPlate() + ": plate is approved");
		}

		Curve curve = getCurve(compound, feature, grouping, false);
		if (curve == null) curve = CurveFactory.newCurve(feature);
		if (curve == null) throw new CurveFitException("Unknown curve kind for feature " + feature);
		
		curve.getCompounds().clear();
		curve.getCompounds().addAll(compounds);
		
		// Apply fit settings, either custom or default.
		curve.setSettings(curveSettingsMgr.getSettings(curve, feature));
		if (grouping.getCount() > 0) curve.getSettings().setGroupBy1(grouping.get(0));
		if (grouping.getCount() > 1) curve.getSettings().setGroupBy2(grouping.get(1));
		if (grouping.getCount() > 2) curve.getSettings().setGroupBy3(grouping.get(2));
		
		// Check if the curve settings are valid.
		CurveProperty[] requiredProperties = { CurveProperty.KIND, CurveProperty.METHOD, CurveProperty.MODEL };
		for (CurveProperty prop: requiredProperties) {
			String settingValue = feature.getCurveSettings().get(prop.toString());
			if (settingValue == null || settingValue.isEmpty()) triggerFitFailed(curve, false, new CurveFitException("Property " + prop + " is not set for feature " + feature));
		}
		
		int validSampleCount = getSampleWells(curve).size();
		if (validSampleCount < MIN_SAMPLES_FOR_FIT) {
			triggerFitFailed(curve, false, new CurveFitException(
					MIN_SAMPLES_FOR_FIT + " samples are required for a dose-response fit."
					+ " Compound " + compounds.get(0).toString() + " has " + validSampleCount + " valid sample(s)."));
		}
		
		// Obtain a fitter and fit the curve.
		ICurveFitter fitter = CurveFactory.getFitter(feature);
		try {
			fitter.fit(curve);
		} catch (CurveFitException e) {
			triggerFitFailed(curve, true, e);
		}

		// Save the results to the database and add to cache.
		curveDAO.saveCurve(curve);
		
		// Update the caches.
		for (Compound comp: curve.getCompounds()) {
			addToCache(comp, feature, grouping, curve);
			curveImageCache.remove(getImageCacheKey(comp, feature, grouping, null, null), true);	
		}

		// Send curve fit event.
		ModelEvent event = new ModelEvent(curve, ModelEventType.CurveFit, 0);
		ModelEventService.getInstance().fireEvent(event);

		return curve;
	}

	/**
	 * Get the input values needed for fitting a curve.
	 * This includes the data points (and their concentrations), but also the lower and upper bounds.
	 */
	public CurveDataPoints getDataPoints(Curve curve) {
		List<Well> wells = getSampleWells(curve);
		
		CurveDataPoints data = new CurveDataPoints();
		data.values = new double[wells.size()];
		data.concs = new double[wells.size()];
		data.accepts = new int[wells.size()];

		int i = 0;
		for (Well well : wells) {
			// Regardless of value, fill out conc and status.
			double conc = well.getCompoundConcentration();
			data.concs[i] = NumberUtils.roundUp(-Math.log10(conc), 3);
			data.accepts[i] = well.getStatus();

			// Fix: drcFit may interpret 0 status incorrectly as invalid.
			if (data.accepts[i] == 0) data.accepts[i] = 1;

			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			double v = dataAccessor.getNumericValue(well, curve.getFeature(), curve.getFeature().getNormalization());
			if (Double.isNaN(v) || Double.isInfinite(v)) {
				data.values[i] = 0;
			} else {
				data.values[i] = v;
			}
			i++;
		}
		
		if (!wells.isEmpty()) {
			Feature f = curve.getFeature();
			String n = f.getNormalization();
			String lowType = ProtocolUtils.getLowType(curve.getFeature());
			String highType = ProtocolUtils.getHighType(curve.getFeature());
			
			if (curve.getCompounds().size() > 1) {
				// Use a stat context which automatically takes care of rejected wells, NaNs, etc.
				List<double[]> lowValues = new ArrayList<>();
				List<double[]> highValues = new ArrayList<>();
				for (Compound c: curve.getCompounds()) {
					if (CompoundValidationStatus.INVALIDATED.matches(c)) continue;
					if (PlateValidationStatus.INVALIDATED.matches(c.getPlate())) continue;
					lowValues.add(StatContextFactory.createContext(new StatQuery(null, c.getPlate(), f, lowType, n)).getData(0));
					highValues.add(StatContextFactory.createContext(new StatQuery(null, c.getPlate(), f, highType, n)).getData(0));
				}
				double[] allLowValues = CollectionUtils.merge(lowValues);
				double[] allHighValues = CollectionUtils.merge(highValues);
				data.lcMean = StatService.getInstance().calculate("mean", allLowValues);
				data.lcMedian = StatService.getInstance().calculate("median", allLowValues);
				data.lcStdev = StatService.getInstance().calculate("stdev", allLowValues);
				data.hcMean = StatService.getInstance().calculate("mean", allHighValues);
				data.hcMedian = StatService.getInstance().calculate("median", allHighValues);
				data.hcStdev = StatService.getInstance().calculate("stdev", allHighValues);
			} else {
				Plate plate = wells.get(0).getPlate();
				data.lcMean = StatService.getInstance().calculate("mean", plate, f, lowType, n);
				data.lcMedian = StatService.getInstance().calculate("median", plate, f, lowType, n);
				data.lcStdev = StatService.getInstance().calculate("stdev", plate, f, lowType, n);
				data.hcMean = StatService.getInstance().calculate("mean", plate, f, highType, n);
				data.hcMedian = StatService.getInstance().calculate("median", plate, f, highType, n);
				data.hcStdev = StatService.getInstance().calculate("stdev", plate, f, highType, n);
			}
		}

		return data;
	}
	
	/**
	 * Get a list of wells that contribute data points to a curve.
	 */
	public List<Well> getSampleWells(Curve curve) {
		Stream<Well> wellStream = streamableList(curve.getCompounds()).stream()
			.filter(c -> !CompoundValidationStatus.INVALIDATED.matches(c))
			.filter(c -> !PlateValidationStatus.INVALIDATED.matches(c.getPlate()))
			.flatMap(c -> streamableList(c.getWells()).stream())
			.filter(w -> isValidDataPoint(w));
		
		CurveGrouping grouping = getGrouping(curve);
		if (grouping == NO_GROUPING) {
			return wellStream.collect(Collectors.toList());
		} else {
			return wellStream
					.filter(w -> getGrouping(w, curve.getFeature()).equals(grouping))
					.collect(Collectors.toList());
		}
	}

	/**
	 * Get the curve's result value (pIC50 or pLAC), formatted in a standard way, including
	 * a censor if applicable.
	 */
	public String getCurveDisplayValue(Curve curve) {
		if (curve == null) return "";
		String cens = "";
		double conc = 0;
		if (curve instanceof OSBCurve) {
			OSBCurve osb = (OSBCurve)curve;
			if (osb.getPic50Censor() != null) cens = osb.getPic50Censor();
			conc = osb.getPic50();
		} else if (curve instanceof PLACCurve) {
			PLACCurve plac = (PLACCurve)curve;
			if (plac.getPlacCensor() != null) cens = plac.getPlacCensor();
			conc = plac.getPlac();
		}
		return cens + Formatters.getInstance().format(conc, "0.###");
	}

	/**
	 * Get the curve grouping that would be used to fit a curve on this sample.
	 * This depends on the feature's group-by configuration.
	 */
	public CurveGrouping getGrouping(Well w, Feature f) {
		String[] keys = {
				f.getCurveSettings().get(CurveSettings.GROUP_BY_1),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_2),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_3)
		};
		if (keys[0] == null && keys[1] == null && keys[2] == null) return NO_GROUPING;
		
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(w.getPlate());
		String[] values = new String[3];
		for (int i = 0; i < values.length; i++) {
			if (keys[i] == null) continue;
			Feature groupFeature = ProtocolUtils.getFeatureByName(keys[i], f.getProtocolClass());
			if (groupFeature == null) continue;
			if (groupFeature.isNumeric()) {
				values[i] = "" + accessor.getNumericValue(w, groupFeature, null);
			} else {
				values[i] = accessor.getStringValue(w, groupFeature);
			}
		}
		return new CurveGrouping(keys, values);
	}

	/**
	 * Get the curve grouping that was used to fit the curve.
	 * This depends on the feature's group-by configuration.
	 */
	public CurveGrouping getGrouping(Curve c) {
		Feature f = c.getFeature();
		String[] keys = {
				f.getCurveSettings().get(CurveSettings.GROUP_BY_1),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_2),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_3)
		};
		String[] values = {
				c.getSettings().getGroupBy1(),
				c.getSettings().getGroupBy2(),
				c.getSettings().getGroupBy3()
		};
		return new CurveGrouping(keys, values);
	}
	
	/**
	 * Get the curve groupings that would be used to fit the samples of this compound.
	 * This only returns more than 1 grouping if the feature has a group-by configuration.
	 */
	public CurveGrouping[] getGroupings(Compound c, Feature f) {
		String[] keys = {
				f.getCurveSettings().get(CurveSettings.GROUP_BY_1),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_2),
				f.getCurveSettings().get(CurveSettings.GROUP_BY_3)
		};
		if (keys[0] == null && keys[1] == null && keys[2] == null) return new CurveGrouping[] { NO_GROUPING };
		
		return streamableList(c.getWells()).stream()
				.map(w -> getGrouping(w, f))
				.distinct()
				.toArray(i -> new CurveGrouping[i]);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void triggerFitFailed(Curve curve, boolean saveCurve, CurveFitException exception) throws CurveFitException {
		if (saveCurve) curveDAO.saveCurve(curve);
		else curveDAO.deleteCurve(curve);

		// Send curve fit failed event.
		ModelEvent event = new ModelEvent(curve, ModelEventType.CurveFitFailed, 0);
		ModelEventService.getInstance().fireEvent(event);

		throw exception;
	}

	private void addToCache(Curve c) {
		Feature feature = c.getFeature();
		CurveGrouping grouping = getGrouping(c);
		for (Compound comp: c.getCompounds()) addToCache(comp, feature, grouping, c);
	}
	
	private void addToCache(Compound c, Feature f, CurveGrouping grouping, Curve curve) {
		CacheKey key = getCacheKey(c, f, grouping);
		curveCache.put(key, curve);
		if (curve != null) curveIdCache.put(curve.getId(), key);
	}
	
	private CacheKey getCacheKey(Compound compound, Feature feature, CurveGrouping grouping) {
		Object[] keyParts = new Object[2 + grouping.getCount()];
		keyParts[0] = compound.getId();
		keyParts[1] = feature.getId();
		for (int i = 0; i < grouping.getCount(); i++) keyParts[2+i] = grouping.get(i);
		return CacheKey.create(keyParts);
	}
	
	private CacheKey getImageCacheKey(Compound compound, Feature feature, CurveGrouping grouping, Integer w, Integer h) {
		Object[] keyParts = new Object[4 + grouping.getCount()];
		keyParts[0] = compound.getId();
		keyParts[1] = feature.getId();
		keyParts[2] = w;
		keyParts[3] = h;
		for (int i = 0; i < grouping.getCount(); i++) keyParts[4+i] = grouping.get(i);
		return CacheKey.create(keyParts);
	}
	
	private Compound getAnyCompound(Curve curve) {
		if (curve.getCompounds() == null || curve.getCompounds().isEmpty()) return null;
		return curve.getCompounds().get(0);
	}
	
	private boolean isValidDataPoint(Well well) {
		if (well.getCompoundConcentration() == 0.0) return false;
		
		int[] invalidCodes = {
				WellStatus.REJECTED_OUTLIER_PHAEDRA.getCode(),
				WellStatus.REJECTED_DATACAPTURE.getCode(),
				WellStatus.REJECTED_PLATEPREP.getCode() };
		if (CollectionUtils.find(invalidCodes, well.getStatus()) >= 0) return false;
		
		// Note: welltype is not checked here because in some rare cases,
		// users may wish to fit curves on controls or empty wells.

		return true;
	}
	
	/*
	 * ******************************************
	 * Supported kinds, methods, models and types
	 * ******************************************
	 */

	public static enum CurveKind {
		PLAC, OSB
	}

	public CurveKind[] getKinds() {
//		return CurveKind.values();
		return new CurveKind[] { CurveKind.OSB };
	}

	public static enum CurveMethod {
		OLS(CurveKind.OSB), LIN(CurveKind.OSB), CENS(CurveKind.OSB),
		SPLINE(CurveKind.PLAC), LINE(CurveKind.PLAC);

		private CurveKind kind;

		private CurveMethod(CurveKind kind) {
			this.kind = kind;
		}

		public CurveKind getKind() {
			return kind;
		}
	}

	public CurveMethod[] getCurveMethods(CurveKind kind) {
		List<CurveMethod> methods = new ArrayList<CurveMethod>();
		for (CurveMethod method : CurveMethod.values()) {
			if (method.getKind() == kind)
				methods.add(method);
		}
		return methods.toArray(new CurveMethod[methods.size()]);
	}

	public static enum CurveModel {
		PL2(CurveKind.OSB), PL3L(CurveKind.OSB), PL3U(CurveKind.OSB), PL4(CurveKind.OSB),
		PL2_R(CurveKind.OSB), PL3L_R(CurveKind.OSB), PL3U_R(CurveKind.OSB), PL4_R(CurveKind.OSB),
		PL2H1(CurveKind.OSB), PL3LH1(CurveKind.OSB), PL3UH1(CurveKind.OSB), PL4H1(CurveKind.OSB),
		PL2H1_R(CurveKind.OSB), PL3LH1_R(CurveKind.OSB), PL3UH1_R(CurveKind.OSB), PL4H1_R(CurveKind.OSB),
		PLOTONLY(CurveKind.OSB),
		SIGMA(CurveKind.PLAC), EMAX(CurveKind.PLAC), ABSOLUTE(CurveKind.PLAC), RELATIVE(CurveKind.PLAC);

		private CurveKind kind;

		private CurveModel(CurveKind kind) {
			this.kind = kind;
		}

		public CurveKind getKind() {
			return kind;
		}
		
		public static CurveModel getByName(String name) {
			for (CurveModel model: values()) {
				if (model.toString().equals(name)) return model;
			}
			return null;
		}
	}

	public CurveModel[] getCurveModels(CurveKind kind) {
		List<CurveModel> models = new ArrayList<CurveModel>();
		for (CurveModel model : CurveModel.values()) {
			if (model.getKind() == kind)
				models.add(model);
		}
		return models.toArray(new CurveModel[models.size()]);
	}

	public static enum CurveType {
		A, D
	}

	public CurveType[] getCurveTypes() {
		return CurveType.values();
	}
}
