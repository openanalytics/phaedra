package eu.openanalytics.phaedra.model.curve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.cache.CacheConfig;
import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ExtensionUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.curve.dao.CurveDAO;
import eu.openanalytics.phaedra.model.curve.dao.CustomSettingsDAO;
import eu.openanalytics.phaedra.model.curve.dao.CustomSettingsDAO.CustomCurveSettings;
import eu.openanalytics.phaedra.model.curve.render.BaseCurveRenderer;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;


/**
 * API for interaction with dose-response curves. This includes:
 * <ul>
 * <li>Fitting dose-response curves</li>
 * <li>Getting and setting custom fit settings for individual curves</li>
 * <li>Retrieving properties of curve fits</li>
 * <li>Retrieving plots of curve fits</li>
 * </ul>
 * All data that is retrieved once, is cached for future use.
 * See {@link CacheService} for more information about caching.
 */
public class CurveFitService extends BaseJPAService {

	private static final CurveGrouping NO_GROUPING = new CurveGrouping(null, null);
	
	private static CurveFitService instance = new CurveFitService();
	
	private CurveDAO curveDAO;
	private CustomSettingsDAO customSettingsDAO;
	
	private ICache curveCache;
	private ICache curveIdCache;
	private ICache curveImageCache;
	private ICache curveCustomSettingsCache;
	
	private String[] knownModelIds;
	private List<ICurveFitModelFactory> modelFactories;
	private List<ICurveRenderer> curveRenderers;
	
	private CurveFitService() {
		// Hidden constructor
		curveDAO = new CurveDAO(getEntityManager());
		customSettingsDAO = new CustomSettingsDAO(getEntityManager());
		
		curveCache = CacheService.getInstance().createCache(new CacheConfig("CurveCache", false));
		curveIdCache = CacheService.getInstance().createCache("CurveIdCache");
		curveImageCache = CacheService.getInstance().createCache("CurveImageCache");
		curveCustomSettingsCache = CacheService.getInstance().createCache("CurveCustomSettingsCache");
		
		Set<String> knownIds = new HashSet<>();
		Arrays.stream(Platform.getExtensionRegistry().getConfigurationElementsFor(ICurveFitModel.EXT_PT_ID))
			.map(e -> e.getAttribute(ICurveFitModel.ATTR_ID)).forEach(id -> knownIds.add(id));
		
		modelFactories = ExtensionUtils.createInstanceList(ICurveFitModelFactory.EXT_PT_ID, ICurveFitModelFactory.ATTR_CLASS, ICurveFitModelFactory.class);
		modelFactories.stream().flatMap(f -> Arrays.stream(f.getSupportedModelIds())).forEach(id -> knownIds.add(id));
		
		knownModelIds = knownIds.toArray(new String[knownIds.size()]);
		Arrays.sort(knownModelIds);
		
		curveRenderers = ExtensionUtils.createInstanceList(ICurveRenderer.EXT_PT_ID, ICurveRenderer.ATTR_CLASS, ICurveRenderer.class);
	}

	public static CurveFitService getInstance() {
		return instance;
	}
	
	/**
	 * Retrieve a dose-response curve by its primary ID.
	 * 
	 * @param curveId The primary ID of the curve to retrieve.
	 * @return The matching curve, or null if no match was found.
	 */
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

	/**
	 * Get a dose-response curve by a well and feature.
	 *  
	 * @param well The well whose data point is in the curve.
	 * @param feature The feature the curve was fit for.
	 * @return The matching curve, or null if no match was found.
	 */
	public Curve getCurve(Well well, Feature feature) {
		CurveGrouping grouping = getGrouping(well, feature);
		return getCurve(well.getCompound(), feature, grouping, false);
	}
	
	/**
	 * Get a curve for a given compound, feature, and grouping.
	 * See {@link CurveGrouping} for more information about grouping.
	 * 
	 * @param compound The compound to retrieve a curve for.
	 * @param feature The well feature to retrieve a curve for.
	 * @param grouping The grouping to retrieve a curve for (may be null).
	 * @param batchMode If true, all dose-response curves of the whole plate will be retrieved
	 * and cached for better performance.
	 * @return The matching curve, or null if no match was found.
	 */
	public Curve getCurve(Compound compound, Feature feature, CurveGrouping grouping, boolean batchMode) {
		// If the feature is incompatible, abort now.
		if (compound == null || feature == null || !PlateUtils.isSameProtocolClass(compound, feature)) {
			return null;
		}
		if (grouping == null) grouping = NO_GROUPING;

		// First look in the cache.
		CacheKey key = getCacheKey(compound, feature, grouping);
		if (curveCache.contains(key)) return (Curve) curveCache.get(key);
		
		synchronized (this) {
			if (curveCache.contains(key)) return (Curve) curveCache.get(key);
			
			// Cache miss, then look in the database.
			Curve curve = null;
			if (batchMode) {
				// Fill cache with nulls, to deal with single-dose compounds which are not returned by the batch query below.
				List<Feature> curveFeatures = CollectionUtils.findAll(PlateUtils.getFeatures(compound), CurveUtils.FEATURES_WITH_CURVES);
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

	/**
	 * Get the image of a dose-response curve.
	 * 
	 * @param curveId The primary ID of the curve.
	 * @param w The width to render the image on.
	 * @param h The height to render the image on.
	 * @return An image representing the dose-response curve.
	 */
	public ImageData getCurveImage(long curveId, int w, int h) {
		CacheKey key = getImageCacheKey(curveId, w, h);
		if (curveImageCache.contains(key)) return (ImageData) curveImageCache.get(key);

		Curve curve = getCurve(curveId);
		if (curve == null) {
			curveImageCache.put(key, null);
			return null;
		}
		
		ImageData image = null;
		synchronized (curve) {
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
	
	/**
	 * Fit (or re-fit) all dose-response curves in a plate.
	 * <p>
	 * Note: some or all of the curves may fail to fit, for example if the plate is already approved.
	 * This will generate warning messages, but not throw any exception.
	 * </p>
	 * 
	 * @param plate The plate whose curves will be fit.
	 */
	public void fitCurves(Plate plate) {
		List<Feature> features = CollectionUtils.findAll(ProtocolUtils.getFeatures(plate), CurveUtils.FEATURES_WITH_CURVES);
		List<Compound> compounds = streamableList(plate.getCompounds()).stream().filter(c -> c.getWells().size() > 1).collect(Collectors.toList());
		if (features.isEmpty() || compounds.isEmpty()) return;
		
		EclipseLog.info("Fitting all curves for " + plate, Activator.getDefault());
		
		List<Object[]> curvesToFit = new ArrayList<>();
		for (Feature feature: features) {
			for (Compound compound: compounds) {
				curvesToFit.add(new Object[] { compound, feature });
			}
		}
		
		Consumer<Object[]> fitter = o -> {
			try {
				Compound compound = (Compound) o[0];
				Feature feature = (Feature) o[1];
				CurveGrouping[] groupings = getGroupings(compound, feature);
				for (CurveGrouping grouping: groupings) {
					fitCurve(compound, feature, grouping);
				}
			} catch (CurveFitException e) {
				EclipseLog.error("Curve fit failed for " + o[0] + " @ " + o[1] + ": " + e.getMessage(), null, Activator.getDefault());
			}
		};

		// When launched from main thread, this can deadlock, because R nodes do a Display.syncExec when starting up.
		if (Thread.currentThread() == Display.getDefault().getThread()) {
			EclipseLog.warn("Batch curve fitting called from the main thread. Fitting single-threaded to avoid UI deadlock.", Activator.getDefault());
			curvesToFit.stream().forEach(fitter);
		} else {
			curvesToFit.parallelStream().forEach(fitter);
		}
	}

	/**
	 * Fit all curves for a given compound and feature.
	 * Note: if the feature has no grouping configured, this will fit just one single curve.
	 * 
	 * @param compound The compound to fit curves for.
	 * @param feature The well feature to fit curves for.
	 * @throws CurveFitException If the fit fails for any reason.
	 */
	public void fitCurves(Compound compound, Feature feature) throws CurveFitException {
		Assert.isLegal(compound != null);
		Assert.isLegal(feature != null);
		
		CurveGrouping[] groupings = getGroupings(compound, feature);
		for (CurveGrouping grouping: groupings) {
			fitCurve(compound, feature, grouping);
		}
	}
	
	/**
	 * Re-fit a dose-response curve.
	 * 
	 * @param curve The curve to fit.
	 * @throws CurveFitException If the fit fails for any reason.
	 */
	public void fitCurve(Curve curve) throws CurveFitException {
		fitCurve(curve.getCompounds().get(0), curve.getFeature(), getGrouping(curve));
	}
	
	/**
	 * Fit a dose-response curve for the given compound, well feature and grouping.
	 * 
	 * @param compound The compound to fit a curve for.
	 * @param feature The well feature to fit a curve for.
	 * @param grouping The grouping to fit for, may be null.
	 * @return The dose-response curve that was fit.
	 * @throws CurveFitException If the fit fails for any reason.
	 */
	public Curve fitCurve(Compound compound, Feature feature, CurveGrouping grouping) throws CurveFitException {
		Assert.isLegal(compound != null, "Compound cannot be null");
		Assert.isLegal(feature != null, "Feature cannot be null");
		
		List<Compound> compounds = CalculationService.getInstance().getMultiploCompounds(compound);
		if (compounds.isEmpty()) throw new CurveFitException("No compound(s) to fit");
		for (Compound c: compounds) {
			if (PlateValidationStatus.INVALIDATED.matches(c.getPlate())) continue;
			if (!PlateCalcStatus.CALCULATION_OK.matches(c.getPlate())) throw new CurveFitException(c.getPlate() + ": plate calculation is not OK");
			if (PlateApprovalStatus.APPROVED.matches(c.getPlate())) throw new CurveFitException(c.getPlate() + ": plate is approved");
		}
		
		Curve curve = getCurve(compound, feature, grouping, false);
		if (curve == null) curve = new Curve();
		curve.setCompounds(compounds);
		curve.setFeature(feature);
		
		CurveFitSettings settings = getSettings(curve);
		if (settings == null) throw new CurveFitException("Cannot fit: no curve fit defined for feature " + feature);
		ICurveFitModel model = getModel(settings.getModelId());
		if (model == null) throw new CurveFitException("Cannot fit: unknown model: " + settings.getModelId());
		
		curve.setFitDate(new Date());
		curve.setModelId(model.getId());
		curve.setGroupingValues(Stream.iterate(0, i -> i+1).limit(grouping.getCount()).map(i -> grouping.get(i)).toArray(i -> new String[i]));

		CurveFitInput input = createInput(compounds, feature, grouping);
		input.setSettings(settings);
		boolean validated = false;
		try {
			model.validateInput(input);
			validated = true;
			model.fit(input, curve);
		} catch (CurveFitException e) {
			triggerFitFailed(curve, validated, e);
		}
		
		curveDAO.saveCurve(curve);
		for (Compound comp: curve.getCompounds()) {
			addToCache(comp, feature, grouping, curve);
			curveImageCache.remove(getImageCacheKey(curve.getId(), null, null), true);	
		}

		// Send curve fit event.
		ModelEvent event = new ModelEvent(curve, ModelEventType.CurveFit, 0);
		ModelEventService.getInstance().fireEvent(event);

		return curve;
	}
	
	/**
	 * Get the default curve fit settings for the given feature.
	 * <p>
	 * Note that an individual dose-response curve may override these settings.
	 * See {@link CurveFitService#getSettings(Curve)}.
	 * </p>
	 * 
	 * @param feature The feature to get curve fit settings for.
	 * @return Tne fit settings for the given feature, or null if the feature has no curve fit settings.
	 */
	public CurveFitSettings getSettings(Feature feature) {
		if (feature.getCurveSettings() == null) return null;
		String modelId = feature.getCurveSettings().get(CurveFitSettings.MODEL);
		if (modelId == null || modelId.isEmpty()) return null;
		
		CurveFitSettings settings = new CurveFitSettings();
		settings.setModelId(modelId);
		
		if (feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_1) != null) {
			settings.setGroupingFeatures(new String[] {
					feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_1),
					feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_2),
					feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_3)
			});
		}
		
		ICurveFitModel model = getModel(modelId);
		if (model == null) return null;
		
		CurveParameter.Value[] extraParams = new CurveParameter.Value[model.getInputParameters().length];
		for (int i = 0; i < extraParams.length; i++) {
			extraParams[i] = CurveParameter.createValue(feature, model.getInputParameters()[i]);
		}
		settings.setExtraParameters(extraParams);
		
		return settings;
	}
	
	/**
	 * Get the overriding curve fit settings for the given dose-response curve,
	 * or the feature's default fit settings if the curve has no overriding settings.
	 * 
	 * @param curve The curve to get overriding settings for.
	 * @return Tne fit settings for the given curve
	 */
	public CurveFitSettings getSettings(Curve curve) {
		if (curve == null) return null;
		if (curve.getId() == 0) return getSettings(curve.getFeature());
		
		CacheKey key = new CacheKey(curve.getId());
		if (!curveCustomSettingsCache.contains(key)) {
			List<CustomCurveSettings> customSettings = customSettingsDAO.loadSettings(curve.getCompounds().get(0).getPlate());
			for (CustomCurveSettings s: customSettings) {
				curveCustomSettingsCache.put(new CacheKey(s.curveId), s.settings);
			}
		}
		
		CurveFitSettings settings = (CurveFitSettings) curveCustomSettingsCache.get(key);
		return (settings == null) ? getSettings(curve.getFeature()) : settings;
	}

	/**
	 * Save the overriding curve fit settings for the given dose-response curve.
	 * 
	 * @param curve The curve to save settings for.
	 * @param settings The settings to save, or null to clear any previous settings.
	 */
	public void updateCurveSettings(Curve curve, CurveFitSettings settings) {
		for (Compound c: curve.getCompounds()) {
			if (PlateApprovalStatus.APPROVED.matches(c.getPlate())) throw new IllegalStateException("Cannot change settings: plate is approved");
		}
		
		if (settings != null) {
			// If new settings are identical to the defaults, treat it as a reset.
			CurveFitSettings defaults = getSettings(curve.getFeature());
			if (defaults.equals(settings)) settings = null;
		}
		
		customSettingsDAO.clearSettings(curve.getId());
		CacheKey key = new CacheKey(curve.getId());
		if (settings == null) {
			curveCustomSettingsCache.remove(key);
		} else {
			customSettingsDAO.saveSettings(curve.getId(), settings);
			curveCustomSettingsCache.put(key, settings);
		}
	}

	/**
	 * Get a list of IDs of supported fit models.
	 * 
	 * @return The IDs of supported fit models.
	 */
	public String[] getFitModels() {
		return knownModelIds;
	}
	
	/**
	 * Get the fit model for the given model ID.
	 * 
	 * @param modelId The ID of the fit model.
	 * @return The matching fit model, or null if no match was found.
	 */
	public ICurveFitModel getModel(String modelId) {
		return modelFactories.stream()
				.filter(f -> CollectionUtils.contains(f.getSupportedModelIds(), modelId)).map(f -> f.createModel(modelId)).findAny()
				.orElse(ExtensionUtils.createInstance(ICurveFitModel.EXT_PT_ID, ICurveFitModel.ATTR_ID, modelId, ICurveFitModel.ATTR_CLASS, ICurveFitModel.class));
	}

	/**
	 * Get a renderer that can draw curves that were fit by the given model.
	 * 
	 * @param modelId The ID of the model used in the fit.
	 * @return A compatible renderer, possibly null.
	 */
	public ICurveRenderer getRenderer(String modelId) {
		return curveRenderers.stream()
				.filter(r -> CollectionUtils.contains(r.getSupportedModelIds(), modelId)).findAny()
				.orElse(new BaseCurveRenderer());
	}
	
	/**
	 * Get the input (data points and settings) that was used for fitting the given curve.
	 *  
	 * @param curve The curve whose input should be retrieved.
	 * @return The input used for the given curve.
	 */
	public CurveFitInput getInput(Curve curve) {
		CurveFitInput input = createInput(curve.getCompounds(), curve.getFeature(), getGrouping(curve));
		input.setSettings(getSettings(curve));
		return input;
	}
	
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	private void triggerFitFailed(Curve curve, boolean saveCurve, CurveFitException exception) throws CurveFitException {
		if (saveCurve) curveDAO.saveCurve(curve);
		else curveDAO.deleteCurve(curve);

		// Send curve fit failed event.
		ModelEvent event = new ModelEvent(curve, ModelEventType.CurveFitFailed, 0);
		ModelEventService.getInstance().fireEvent(event);

		throw exception;
	}
	
	private CacheKey getCacheKey(Compound compound, Feature feature, CurveGrouping grouping) {
		if (grouping == null) grouping = NO_GROUPING;
		Object[] keyParts = new Object[2 + grouping.getCount()];
		keyParts[0] = compound.getId();
		keyParts[1] = feature.getId();
		for (int i = 0; i < grouping.getCount(); i++) keyParts[2+i] = grouping.get(i);
		return CacheKey.create(keyParts);
	}
	
	private CacheKey getImageCacheKey(long curveId, Integer w, Integer h) {
		Object[] keyParts = new Object[3];
		keyParts[0] = curveId;
		keyParts[1] = w;
		keyParts[2] = h;
		return CacheKey.create(keyParts);
	}
	
	private void addToCache(Curve curve) {
		Feature feature = curve.getFeature();
		CurveGrouping grouping = getGrouping(curve);
		for (Compound comp: curve.getCompounds()) addToCache(comp, feature, grouping, curve);
	}
	
	private void addToCache(Compound compound, Feature feature, CurveGrouping grouping, Curve curve) {
		CacheKey key = getCacheKey(compound, feature, grouping);
		curveCache.put(key, curve);
		if (curve != null) curveIdCache.put(curve.getId(), key);
	}
	
	private CurveFitInput createInput(List<Compound> compounds, Feature feature, CurveGrouping grouping) {
		Stream<Well> wellStream = streamableList(compounds).stream()
				.filter(c -> !CompoundValidationStatus.INVALIDATED.matches(c))
				.filter(c -> !PlateValidationStatus.INVALIDATED.matches(c.getPlate()))
				.flatMap(c -> streamableList(c.getWells()).stream())
				.filter(w -> isValidDataPoint(w));
		
		List<Well> wells = null;
		if (grouping == null || grouping.equals(NO_GROUPING)) {
			wells = wellStream.collect(Collectors.toList());
		} else {
			wells = wellStream
					.filter(w -> getGrouping(w, feature).equals(grouping))
					.collect(Collectors.toList());
		}
		
		CurveFitInput input = new CurveFitInput();
		input.setWells(wells);
		
		double[] values = new double[wells.size()];
		double[] concs = new double[wells.size()];
		boolean[] accepts = new boolean[wells.size()];

		for (int i = 0; i < wells.size(); i++) {
			Well well = wells.get(i);
			
			// Regardless of value, fill out conc and status.
			double conc = well.getCompoundConcentration();
			concs[i] = NumberUtils.roundUp(-Math.log10(conc), 3);
			accepts[i] = well.getStatus() >= 0;

			//TODO Special treatment for NaN/Inf?
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			values[i] = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
		}
		
		input.setValues(values);
		input.setConcs(concs);
		input.setValid(accepts);
		return input;
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
	
	private CurveGrouping getGrouping(Curve curve) {
		Feature feature = curve.getFeature();
		String[] keys = {
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_1),
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_2),
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_3)
		};
		String[] values = curve.getGroupingValues();
		return new CurveGrouping(keys, values);
	}
	
	private CurveGrouping getGrouping(Well well, Feature feature) {
		String[] keys = {
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_1),
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_2),
				feature.getCurveSettings().get(CurveFitSettings.GROUP_BY_3)
		};
		if (keys[0] == null && keys[1] == null && keys[2] == null) return NO_GROUPING;
		
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		String[] values = new String[3];
		for (int i = 0; i < values.length; i++) {
			if (keys[i] == null) continue;
			Feature groupFeature = ProtocolUtils.getFeatureByName(keys[i], feature.getProtocolClass());
			if (groupFeature == null) continue;
			if (groupFeature.isNumeric()) {
				values[i] = "" + accessor.getNumericValue(well, groupFeature, null);
			} else {
				values[i] = accessor.getStringValue(well, groupFeature);
			}
		}
		return new CurveGrouping(keys, values);
	}
	
	public CurveGrouping[] getGroupings(Compound c, Feature f) {
		String[] keys = {
				f.getCurveSettings().get(CurveFitSettings.GROUP_BY_1),
				f.getCurveSettings().get(CurveFitSettings.GROUP_BY_2),
				f.getCurveSettings().get(CurveFitSettings.GROUP_BY_3)
		};
		if (keys[0] == null && keys[1] == null && keys[2] == null) return new CurveGrouping[] { NO_GROUPING };
		
		return streamableList(c.getWells()).stream()
				.map(w -> getGrouping(w, f))
				.distinct()
				.toArray(i -> new CurveGrouping[i]);
	}
}
