package eu.openanalytics.phaedra.ui.plate.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeaturePrefixCalculator {

	private Well currentWell;
	private List<String> prefixes;
	private ColumnConfiguration[] columnState;

	public FeaturePrefixCalculator() {
		prefixes = new ArrayList<>();
	}

	public List<String> getPrefixes() {
		return prefixes;
	}

	public void setPrefixes(List<String> prefixes) {
		this.prefixes = prefixes;
	}

	public Well getCurrentWell() {
		return currentWell;
	}

	public void setCurrentWell(Well currentWell) {
		this.currentWell = currentWell;
	}

	public ColumnConfiguration[] getColumnState() {
		return columnState;
	}

	public void setColumnState(ColumnConfiguration[] columnState) {
		this.columnState = columnState;
	}

	public ColumnConfiguration getColumnState(String columnName) {
		if (columnState == null) return null;
		for (ColumnConfiguration savedConfig: columnState) {
			if (savedConfig.getName().equals(columnName)) {
				return savedConfig;
			}
		}
		return null;
	}

	public List<String> getSuffixes() {
		// Build a list of suffixes
		List<String> suffixes = new ArrayList<>();
		if (currentWell == null || prefixes == null) return suffixes;

		List<Feature> features = PlateUtils.getFeatures(currentWell.getPlate());
		for (Feature f: features) {
			String name = f.getName();
			for (String prefix: prefixes) {
				if (name.startsWith(prefix)) {
					String suffix = name.substring(prefix.length());
					CollectionUtils.addUnique(suffixes, suffix);
				}
			}
		}
		if (columnState == null) {
			Collections.sort(suffixes);
		} else {
			// There is a saved column state: check the column order.
			List<String> orderedSuffixes = new ArrayList<>(suffixes);
			for (String suffix: suffixes) {
				int index = -1;
				for (int i=0; i<columnState.length; i++) {
					String name = columnState[i].getName();
					if (suffix.equals(name)) {
						// -1 because columnState includes an empty "" column on the left.
						index = i-1;
						break;
					}
				}
				if (index != -1) orderedSuffixes.set(index, suffix);
				else orderedSuffixes.add(suffix);
			}
			suffixes = orderedSuffixes;
		}
		return suffixes;
	}

	public String getValueFor(String prefix, String suffix) {
		if (currentWell == null) return null;
		String featureName = prefix + suffix;
		Feature f = ProtocolUtils.getFeatureByName(featureName, PlateUtils.getProtocolClass(currentWell));
		if (f == null) return null;

		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(currentWell.getPlate());
		if (f.isNumeric()) {
			double value = accessor.getNumericValue(currentWell, f, f.getNormalization());
			return Formatters.getInstance().format(value, f);
		} else {
			return accessor.getStringValue(currentWell, f);
		}	
	}

}
