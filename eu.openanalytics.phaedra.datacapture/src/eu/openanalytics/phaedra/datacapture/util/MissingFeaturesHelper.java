package eu.openanalytics.phaedra.datacapture.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class MissingFeaturesHelper {

	private IDataCaptureStore store;
	private ProtocolClass pClass;
	private Class<? extends IFeature> featureType;
	
	public MissingFeaturesHelper(IDataCaptureStore store, ProtocolClass pClass, Class<? extends IFeature> featureType) {
		this.store = store;
		this.pClass = pClass;
		this.featureType = featureType;
	}
	
	public String getFeatureTypeName() {
		if (featureType == Feature.class) return "well";
		else return "subwell";
	}
	
	public List<FeatureDefinition> findMissingFeatures() throws DataCaptureException {
		List<FeatureDefinition> capturedFeatures = getCapturedFeatures();
		List<FeatureDefinition> pClassFeatures = getProtocolClassFeatures();
		return capturedFeatures.stream().filter(f -> !pClassFeatures.contains(f)).collect(Collectors.toList());
	}
	
	private List<FeatureDefinition> getCapturedFeatures() throws DataCaptureException {
		if (featureType == Feature.class) {
			return Arrays.stream(store.getWellFeatures())
					.map(k -> new FeatureDefinition(k))
					.collect(Collectors.toList());
		} else {
			return Arrays.stream(store.getSubWellFeatures())
					.map(k -> new FeatureDefinition(k))
					.collect(Collectors.toList());
		}
	}
	
	private List<FeatureDefinition> getProtocolClassFeatures() {
		List<? extends IFeature> features = null;
		if (featureType == Feature.class) {
			features = new ArrayList<>(pClass.getFeatures());
		} else {
			features = new ArrayList<>(pClass.getSubWellFeatures());
		}
		return features.stream().map(f -> {
			FeatureDefinition def = new FeatureDefinition(f.getName());
			def.isNumeric = f.isNumeric();
			def.isKey = f.isKey();
			return def;
		}).collect(Collectors.toList());
	}
	
	public boolean confirmFeatureCreation(List<FeatureDefinition> missingFeatures) {
		if (missingFeatures == null || missingFeatures.isEmpty()) return false;
		
		List<FeatureDefinition> featuresToAdd = missingFeatures.stream().filter(f -> f.addFeatureToProtocolClass).collect(Collectors.toList());
		if (featuresToAdd.isEmpty()) return false;
		
		StringBuilder sb = new StringBuilder();
		int trimCount = 20;
		for (int i=0; i<featuresToAdd.size(); i++) {
			sb.append("\n"+featuresToAdd.get(i).name);
			if (i == trimCount-1 && featuresToAdd.size() > trimCount) {
				sb.append("\n... and " + (featuresToAdd.size()-trimCount) + " more");
				break;
			}
		}
		String featureNames = sb.toString();
		
		final AtomicInteger retVal = new AtomicInteger();
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				String featureTypeName = (featureType == Feature.class) ? "well" : "subwell";
				String[] buttons = { "Yes", "No", "Back" };
				MessageDialog dialog = new MessageDialog(null,
						"Add missing " + featureTypeName + " features", null,
						"Would you like to add the following " + featureTypeName + " features to protocol class " + pClass.getName() + "?\n" + featureNames,
						MessageDialog.QUESTION, buttons, 0);
				retVal.set(dialog.open());
			}
		});
		return retVal.get() == Window.OK;
	}

	public void createMissingFeatures() throws DataCaptureException {
		List<FeatureDefinition> missingFeatures = findMissingFeatures();
		if (missingFeatures.isEmpty()) return;
		
		boolean isHandled = false;
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IMissingFeaturesHandler.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IMissingFeaturesHandler.ATTR_CLASS);
				if (o instanceof IMissingFeaturesHandler) {
					isHandled = ((IMissingFeaturesHandler) o).handle(missingFeatures, this);
				}
			} catch (CoreException e) {
				throw new IllegalArgumentException("Invalid handler: " + el.getAttribute(IMissingFeaturesHandler.ATTR_CLASS));
			}
			if (isHandled) break;
		}
		
		if (!isHandled) createMissingFeatures(missingFeatures);
	}
	
	public void createMissingFeatures(List<FeatureDefinition> missingFeatures) {
		if (missingFeatures.isEmpty()) return;
		if (!ProtocolService.getInstance().canEditProtocolClass(pClass)) return;
		
		Consumer<? super FeatureDefinition> featureCreator = null;
		if (featureType == Feature.class) {
			featureCreator = f -> {
				Feature newFeature = ProtocolService.getInstance().createFeature(pClass);
				newFeature.setName(f.name);
				newFeature.setNumeric(f.isNumeric);
				newFeature.setKey(f.isKey);
				newFeature.setLogarithmic(f.isLogarithmic);
				pClass.getFeatures().add(newFeature);
			};
		} else {
			featureCreator = f -> {
				SubWellFeature newFeature = ProtocolService.getInstance().createSubWellFeature(pClass);
				newFeature.setName(f.name);
				newFeature.setNumeric(f.isNumeric);
				newFeature.setKey(f.isKey);
				newFeature.setLogarithmic(f.isLogarithmic);
				pClass.getSubWellFeatures().add(newFeature);
			};
		}

		List<FeatureDefinition> pClassFeatures = getProtocolClassFeatures();
		missingFeatures.stream()
			.filter(f -> f.addFeatureToProtocolClass)
			.filter(f -> !pClassFeatures.contains(f))
			.forEach(featureCreator);
		ProtocolService.getInstance().updateProtocolClass(pClass);
	}
}
