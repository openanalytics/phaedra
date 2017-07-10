package eu.openanalytics.phaedra.link.data.addfeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class AddFeaturesUtils {

	public static List<FeatureDefinition> calculateMissingFeatures(AddFeaturesTask task, List<PlateReading> readings) {
		List<FeatureDefinition> capturedFeatures = getCapturedFeatures(task, readings);
		List<FeatureDefinition> pClassFeatures = getProtocolClassFeatures(task);
		return capturedFeatures.stream().filter(f -> !pClassFeatures.contains(f)).collect(Collectors.toList());
	}
	
	private static List<FeatureDefinition> getCapturedFeatures(AddFeaturesTask task, List<PlateReading> readings) {
		List<FeatureDefinition> featureDefinitions = new ArrayList<FeatureDefinition>();

		for (PlateReading reading : readings) {
			try (HDF5File hdf5File = HDF5File.openForRead(reading.getCapturePath())) {
				// Collect all unique well feature names found in the files to be imported
				FeatureDefinition def;
				List<String> featureNames = null;
				if (task.featureClass == Feature.class) featureNames = hdf5File.getWellFeatures();
				else featureNames = hdf5File.getSubWellFeatures();
				
				for (String featureName: featureNames) {
					def = new FeatureDefinition(featureName);
					if (task.featureClass == Feature.class) def.isNumeric = hdf5File.isWellDataNumeric(featureName);
					else def.isNumeric = true; //TODO Check if subwelldata is really numeric
					
					// Check that, if the feature is already in the list, it's properties are the same 
					// (there could could be a feature with the same name but different properties in different input files)
					int index = featureDefinitions.indexOf(def);
					if (index >= 0) {
						FeatureDefinition wfdInList = featureDefinitions.get(index);
						if (def.isNumeric != wfdInList.isNumeric) 
							throw new RuntimeException("Feature '" + def.name + "' occurs with different properties (isNumeric) in '" + reading.getCapturePath() + "'.");
					} else {
						featureDefinitions.add(def);
					}
				}
			}
		}
		return featureDefinitions;
	}
	
	private static List<FeatureDefinition> getProtocolClassFeatures(AddFeaturesTask task) {
		List<? extends IFeature> features = null;
		if (task.featureClass == Feature.class) features = new ArrayList<>(task.targetProtocolClass.getFeatures());
		else features = new ArrayList<>(task.targetProtocolClass.getSubWellFeatures());
		
		return features.stream().map(f -> {
			FeatureDefinition def = new FeatureDefinition(f.getName());
			def.isNumeric = f.isNumeric();
			def.isKey = f.isKey();
			return def;
		}).collect(Collectors.toList());
	}
	
	public static int promptFeatureCreation(AddFeaturesTask task) {
		if (task.featureDefinitions == null || task.featureDefinitions.isEmpty()) return Window.CANCEL;
		
		List<FeatureDefinition> featuresToAdd = task.featureDefinitions.stream().filter(f -> f.addFeatureToProtocolClass).collect(Collectors.toList());
		if (featuresToAdd.isEmpty()) return Window.CANCEL;
		
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
				String featureType = (task.featureClass == Feature.class)? "well" : "subwell";
				String[] buttons = { "Yes", "No", "Back" };
				MessageDialog dialog = new MessageDialog(null,
						"Add missing " + featureType + " features", null,
						"Would you like to add the following " + featureType + " features to protocol class " + task.targetProtocolClass.getName() + "?\n" + featureNames,
						MessageDialog.QUESTION, buttons, 0);
				retVal.set(dialog.open());
			}
		});
		return retVal.get();
	}

	public static void createMissingFeatures(AddFeaturesTask task) {
		Consumer<? super FeatureDefinition> featureCreator = null;
		if (task.featureClass == Feature.class) {
			featureCreator = f -> {
				Feature newFeature = ProtocolService.getInstance().createFeature(task.targetProtocolClass);
				newFeature.setName(f.name);
				newFeature.setNumeric(f.isNumeric);
				newFeature.setKey(f.isKey);
				newFeature.setLogarithmic(f.isLogarithmic);
				task.targetProtocolClass.getFeatures().add(newFeature);
			};
		} else {
			featureCreator = f -> {
				SubWellFeature newFeature = ProtocolService.getInstance().createSubWellFeature(task.targetProtocolClass);
				newFeature.setName(f.name);
				newFeature.setNumeric(f.isNumeric);
				newFeature.setKey(f.isKey);
				newFeature.setLogarithmic(f.isLogarithmic);
				task.targetProtocolClass.getSubWellFeatures().add(newFeature);
			};
		}

		List<FeatureDefinition> pClassFeatures = getProtocolClassFeatures(task);
		task.featureDefinitions.stream().filter(f -> f.addFeatureToProtocolClass).filter(f -> !pClassFeatures.contains(f)).forEach(featureCreator);
		ProtocolService.getInstance().updateProtocolClass(task.targetProtocolClass);
	}
}
