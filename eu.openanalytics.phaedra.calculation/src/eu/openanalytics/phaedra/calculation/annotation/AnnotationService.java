package eu.openanalytics.phaedra.calculation.annotation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.Activator;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.ClassificationService.PatternType;
import eu.openanalytics.phaedra.calculation.pref.Prefs;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class AnnotationService {
	
	private static AnnotationService instance = new AnnotationService();

	private AnnotationService() {
		// Hidden constructor
	}

	public static AnnotationService getInstance() {
		return instance;
	}
	
	public Feature createAnnotation(String name, boolean numeric, ProtocolClass pClass, List<FeatureClass> classes) {
		ProtocolService.getInstance().checkCanEditProtocolClass(pClass);
		Feature feature = ProtocolService.getInstance().createFeature(pClass);
		feature.setName(name);
		feature.setNumeric(numeric);
		feature.setAnnotation(true);
		if (classes != null) {
			// If classes are provided, create copies and add to the feature.
			for (FeatureClass fClass: classes) {
				FeatureClass copy = ProtocolService.getInstance().createFeatureClass();
				ObjectCopyFactory.copySettings(fClass, copy, false);
				feature.getFeatureClasses().add(copy);
				if (copy.getLabel() == null || copy.getLabel().isEmpty()) copy.setLabel(copy.getPattern());
			}
			if (!classes.isEmpty()) feature.setClassificationRestricted(true);
		}
		pClass.getFeatures().add(feature);
		
		// Update 1: save the feature and the classes
		try {
			ProtocolService.getInstance().updateProtocolClass(pClass);
		} catch (Exception e) {
			pClass.getFeatures().remove(feature);
			throw e;
		}
		
		for (FeatureClass fClass: feature.getFeatureClasses()) fClass.setWellFeature(feature);
		feature.getColorMethodSettings().put("method.id", "eu.openanalytics.phaedra.ui.protocol.colormethod.ClassificationColorMethod");
		feature.getColorMethodSettings().put("feature_id", "" + feature.getId());
		
		// Update 2: link the classes and the colormethod to the feature (using its newly generated id).
		ProtocolService.getInstance().updateProtocolClass(pClass);
		return feature;
	}
	
	public void applyAnnotations(Plate plate, Function<Well,Set<String>> annotationGetter, BiFunction<Well, String, Object> valueGetter) {
		
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(plate);
		int wellCount = PlateUtils.getWellCount(plate);
		
		Set<String> allAnnotations = new HashSet<>();
		Map<String, Set<String>> undefinedAnnotationValues = new HashMap<>();
		Map<String, Boolean> undefinedAnnotationNumeric = new HashMap<>();
		
		// Collect undefined annotations and values
		for (Well well: plate.getWells()) {
			Set<String> annotations = annotationGetter.apply(well);
			allAnnotations.addAll(annotations);
		}
		for (String ann: allAnnotations) {
			Feature f = ProtocolUtils.getFeatureByName(ann, pClass);
			if (f != null) continue;
			
			boolean allNumeric = true;
			Set<String> distinctValues = new HashSet<>();
			
			for (Well well: plate.getWells()) {
				Object value = valueGetter.apply(well, ann);
				if (value == null) continue;
				String stringValue = value.toString();
				if (!NumberUtils.isDouble(stringValue)) allNumeric = false;
				distinctValues.add(stringValue);
			}
			
			undefinedAnnotationValues.put(ann, distinctValues);
			undefinedAnnotationNumeric.put(ann, allNumeric);
		}
		
		if (!undefinedAnnotationValues.isEmpty()
				&& Activator.getDefault().getPreferenceStore().getBoolean(Prefs.AUTO_DEFINE_ANNOTATIONS)
				&& ProtocolService.getInstance().canEditProtocolClass(pClass)) {
			getAnnotationCreator().create(pClass, undefinedAnnotationValues, undefinedAnnotationNumeric);
		}
		
		// Update feature values where applicable
		boolean valuesModified = false;
		for (String name: allAnnotations) {
			Feature f = ProtocolUtils.getFeatureByName(name, pClass);
			if (f == null) continue;
			valuesModified = true;
			
			if (f.isNumeric()) {
				double[] values = new double[wellCount];
				for (Well well: plate.getWells()) {
					int wellNr = PlateUtils.getWellNr(well);
					Object value = valueGetter.apply(well, name);
					if (value instanceof Number) {
						values[wellNr-1] = ((Number) value).doubleValue();
					} else if (value instanceof String && NumberUtils.isDouble((String) value)) {
						values[wellNr-1] = Double.parseDouble((String) value);
					} else {
						values[wellNr-1] = Double.NaN;
					}
				}
				PlateService.getInstance().updateWellDataRaw(plate, f, values);
			} else {
				String[] values = new String[wellCount];
				for (Well well: plate.getWells()) {
					int wellNr = PlateUtils.getWellNr(well);
					Object value = valueGetter.apply(well, name);
					if (value != null) values[wellNr-1] = value.toString();
				}
				PlateService.getInstance().updateWellDataRaw(plate, f, values);
			}
		}
		if (valuesModified) CalculationService.getInstance().getAccessor(plate).reset();
	}
	
	public FeatureClass createValueClass(String name) {
		FeatureClass fClass = ProtocolService.getInstance().createFeatureClass();
		fClass.setPatternType(PatternType.Literal.getName());
		fClass.setLabel("");
		fClass.setPattern(name);
		fClass.setRgbColor(0x0000C8);
		return new TransientFeatureClass(fClass);
	}
	
	private IAnnotationCreator getAnnotationCreator() {
		Set<IAnnotationCreator> creators = new HashSet<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IAnnotationCreator.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				IAnnotationCreator creator = (IAnnotationCreator) el.createExecutableExtension(IAnnotationCreator.ATTR_CLASS);
				creators.add(creator);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
		return creators.stream().max((c1, c2) -> c1.getPriority() - c2.getPriority()).orElse(new DefaultAnnotationCreator());
	}
	
	private static class TransientFeatureClass extends FeatureClass {
		
		private static final long serialVersionUID = -8743529025777830018L;

		public TransientFeatureClass(FeatureClass from) {
			ObjectCopyFactory.copySettings(from, this, false);
		}
		
		@Override
		public boolean equals(Object obj) {
			return (this == obj);
		}
	}
}
