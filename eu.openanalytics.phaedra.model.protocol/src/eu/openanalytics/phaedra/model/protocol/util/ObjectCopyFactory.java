package eu.openanalytics.phaedra.model.protocol.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * This factory allows copying of value objects.
 * It is similar to cloning, but also allows copying values onto existing
 * objects (overwriting them).
 * The copy is "deep": collections and references are also replaced with copies.
 */
public class ObjectCopyFactory {

	public static void copySettings(ProtocolClass from, ProtocolClass to) {
		copySettings(from, to, true);
	}

	public static void copySettings(ProtocolClass from, ProtocolClass to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setDefaultTemplate(from.getDefaultTemplate());
		to.setEditable(from.isEditable());
		to.setInDevelopment(from.isInDevelopment());
		to.setHighWellTypeCode(from.getHighWellTypeCode());
		to.setDefaultLinkSource(from.getDefaultLinkSource());
		to.setDefaultCaptureConfig(from.getDefaultCaptureConfig());
		to.setLowWellTypeCode(from.getLowWellTypeCode());
		to.setMultiDimensionalSubwellData(from.isMultiDimensionalSubwellData());
		to.setProtocols(new ArrayList<>(from.getProtocols()));
		
		copySettings(from.getFeatures(), to.getFeatures(), copyIds);
		for (Feature f: to.getFeatures()) {
			f.setProtocolClass(to);
		}

		copySettings(from.getSubWellFeatures(), to.getSubWellFeatures(), copyIds);
		for (SubWellFeature f: to.getSubWellFeatures()) {
			f.setProtocolClass(to);
		}

//		if (to.getFeatureGroups() == null && from.getFeatureGroups() != null) {
//			to.setFeatureGroups(new ArrayList<>(from.getFeatureGroups()));
//		}
		if (from.getFeatureGroups() == null) {
			from.setFeatureGroups(new ArrayList<FeatureGroup>());
		}
		if (to.getFeatureGroups() == null) {
			to.setFeatureGroups(new ArrayList<FeatureGroup>());
		}
		copySettings(from.getFeatureGroups(), to.getFeatureGroups(), copyIds);
		for (FeatureGroup g: to.getFeatureGroups()) {
			g.setProtocolClass(to);

			// Replace the reference to an original Feature group with a copied one.
			for (Feature f : to.getFeatures()) {;
				if (f.getFeatureGroup() != null	&& f.getFeatureGroup().equals(g)) {
					f.setFeatureGroup(g);
				}
			}

			for (SubWellFeature f : to.getSubWellFeatures()) {
				if (f.getFeatureGroup() != null	&& f.getFeatureGroup().equals(g)) {
					f.setFeatureGroup(g);
				}
			}

		}

		if (from.getImageSettings() == null) {
			from.setImageSettings(new ImageSettings());
			from.getImageSettings().setImageChannels(new ArrayList<ImageChannel>());
		}
		copySettings(from.getImageSettings(), to.getImageSettings(), copyIds);

		if (from.getDefaultFeature() != null) {
			Feature target = null;
			if (copyIds) {
				long id = from.getDefaultFeature().getId();
				target = ProtocolUtils.getFeatures(to).stream().filter(f -> f.getId() == id).findAny().orElse(null);
			} else {
				// No ids available in the "to" list, use name instead.
				for (Feature f: to.getFeatures()) {
					if (f.getName().equals(from.getDefaultFeature().getName())) {
						target = f;
						break;
					}
				}
			}
			to.setDefaultFeature(target);
		} else {
			to.setDefaultFeature(null);
		}
	}

	public static void copySettings(Feature from, Feature to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setCalculationFormula(from.getCalculationFormula());
		to.setCalculationLanguage(from.getCalculationLanguage());
		to.setCalculationTrigger(from.getCalculationTrigger());
		to.setCalculationSequence(from.getCalculationSequence());
		to.setNormalization(from.getNormalization());
		to.setNormalizationLanguage(from.getNormalizationLanguage());
		to.setNormalizationFormula(from.getNormalizationFormula());
		to.setNormalizationScope(from.getNormalizationScope());
		to.setDescription(from.getDescription());
		to.setFormatString(from.getFormatString());
		to.setLowWellTypeCode(from.getLowWellTypeCode());
		to.setHighWellTypeCode(from.getHighWellTypeCode());
		to.setKey(from.isKey());
		to.setLogarithmic(from.isLogarithmic());
		to.setName(from.getName());
		to.setNumeric(from.isNumeric());
		to.setRequired(from.isRequired());
		to.setAnnotation(from.isAnnotation());
		to.setShortName(from.getShortName());
		to.setUploaded(from.isUploaded());
		to.setFeatureGroup(from.getFeatureGroup());
		to.setClassificationRestricted(from.isClassificationRestricted());
		
		if (to.getCurveSettings() == null) to.setCurveSettings(new HashMap<String, String>());
		to.getCurveSettings().clear();
		for (String setting: from.getCurveSettings().keySet()) {
			to.getCurveSettings().put(setting, from.getCurveSettings().get(setting));
		}

		if (to.getColorMethodSettings() == null) to.setColorMethodSettings(new HashMap<String, String>());
		to.getColorMethodSettings().clear();
		for (String setting: from.getColorMethodSettings().keySet()) {
			to.getColorMethodSettings().put(setting, from.getColorMethodSettings().get(setting));
		}

		if (from.getFeatureClasses() == null) from.setFeatureClasses(new ArrayList<FeatureClass>());
		if (to.getFeatureClasses() == null) to.setFeatureClasses(new ArrayList<FeatureClass>());
		copySettings(from.getFeatureClasses(), to.getFeatureClasses(), copyIds);
		for (FeatureClass f: to.getFeatureClasses()) {
			f.setWellFeature(to);
		}
	}

	public static void copySettings(SubWellFeature from, SubWellFeature to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setDescription(from.getDescription());
		to.setFormatString(from.getFormatString());
		to.setKey(from.isKey());
		to.setLogarithmic(from.isLogarithmic());
		to.setName(from.getName());
		to.setNumeric(from.isNumeric());
		to.setShortName(from.getShortName());
		to.setPositionRole(from.getPositionRole());
		if (from.getFeatureClasses() == null) from.setFeatureClasses(new ArrayList<FeatureClass>());
		if (to.getFeatureClasses() == null) to.setFeatureClasses(new ArrayList<FeatureClass>());
		copySettings(from.getFeatureClasses(), to.getFeatureClasses(), copyIds);
		for (FeatureClass f: to.getFeatureClasses()) {
			f.setSubWellFeature(to);
		}
		to.setFeatureGroup(from.getFeatureGroup());
	}

	public static void copySettings(ImageSettings from, ImageSettings to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setGamma(from.getGamma());
		to.setZoomRatio(from.getZoomRatio());
		to.setPixelSizeX(from.getPixelSizeX());
		to.setPixelSizeY(from.getPixelSizeY());
		to.setPixelSizeZ(from.getPixelSizeZ());
		copySettings(from.getImageChannels(), to.getImageChannels(), copyIds);
		for (ImageChannel ic: to.getImageChannels()) {
			ic.setImageSettings(to);
		}
	}

	public static void copySettings(ImageChannel from, ImageChannel to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setSequence(from.getSequence());
		to.setAlpha(from.getAlpha());
		to.setColorMask(from.getColorMask());
		to.setDescription(from.getDescription());
		to.setLevelMax(from.getLevelMax());
		to.setLevelMin(from.getLevelMin());
		to.setLookupHigh(from.getLookupHigh());
		to.setLookupLow(from.getLookupLow());
		to.setName(from.getName());
		to.setShowInPlateView(from.isShowInPlateView());
		to.setShowInWellView(from.isShowInWellView());
		to.setType(from.getType());
		to.setSource(from.getSource());
		to.setBitDepth(from.getBitDepth());

		if (to.getChannelConfig() == null) to.setChannelConfig(new HashMap<String, String>());
		to.getChannelConfig().clear();
		for (String setting: from.getChannelConfig().keySet()) {
			to.getChannelConfig().put(setting, from.getChannelConfig().get(setting));
		}
	}

	public static void copySettings(FeatureClass from, FeatureClass to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setDescription(from.getDescription());
		to.setLabel(from.getLabel());
		to.setPattern(from.getPattern());
		to.setPatternType(from.getPatternType());
		to.setRgbColor(from.getRgbColor());
		to.setSymbol(from.getSymbol());
	}
	
	public static void copySettings(FeatureGroup from, FeatureGroup to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setName(from.getName());
		to.setDescription(from.getDescription());
		to.setType(from.getType());
	}

	private static <E> void copySettings(List<E> from, List<E> to, boolean copyIds) {
		List<E> oldItems = new ArrayList<>(to);
		to.clear();
		for (E newItem: from) {
			E itemToReplace = oldItems.stream().filter(i -> getId(i) == getId(newItem)).findAny().orElse(null);
			if (itemToReplace == null) {
				// This item didn't exist yet.
				try {
					@SuppressWarnings("unchecked")
					E newInstance = (E)newItem.getClass().newInstance();
					copySettings(newItem, newInstance, copyIds);
					to.add(newInstance);
				} catch (Exception e) { e.printStackTrace(); }
			} else {
				copySettings(newItem, itemToReplace, copyIds);
				to.add(itemToReplace);
			}
		}
	}

	private static long getId(Object o) {
		try {
			Method m = o.getClass().getMethod("getId", (Class[])null);
			Object result = m.invoke(o, (Object[])null);
			long id = (Long)result;
			return id;
		} catch (Exception e) {}
		return 0;
	}
	
	private static <E> void copySettings(E from, E to, boolean copyIds) {
		if (from instanceof ProtocolClass) {
			copySettings((ProtocolClass)from, (ProtocolClass)to, copyIds);
		} else if (from instanceof Feature) {
			copySettings((Feature)from, (Feature)to, copyIds);
		} else if (from instanceof FeatureClass) {
			copySettings((FeatureClass)from, (FeatureClass)to, copyIds);
		} else if (from instanceof SubWellFeature) {
			copySettings((SubWellFeature)from, (SubWellFeature)to, copyIds);
		} else if (from instanceof ImageSettings) {
			copySettings((ImageSettings)from, (ImageSettings)to, copyIds);
		} else if (from instanceof ImageChannel) {
			copySettings((ImageChannel)from, (ImageChannel)to, copyIds);
		} else if (from instanceof FeatureGroup) {
			copySettings((FeatureGroup)from, (FeatureGroup)to, copyIds);
		} else {
			throw new IllegalArgumentException("Unsupported type to copy: " + from.getClass());
		}
	}


}
