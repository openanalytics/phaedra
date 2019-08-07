package eu.openanalytics.phaedra.calculation.norm.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.BundleReference;

import eu.openanalytics.phaedra.base.util.misc.FormulaDescriptor;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.UrlFormulaDescriptor;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.INormalizer;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;


public abstract class BaseNormalizer implements INormalizer {
	
	
	private FormulaDescriptor formulaDescr;
	
	
	public BaseNormalizer() {
		formulaDescr = createFormulaDescriptor();
	}
	
	protected FormulaDescriptor createFormulaDescriptor() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (classLoader instanceof BundleReference) {
			try {
				String bundleId = ((BundleReference) classLoader).getBundle().getSymbolicName();
				return new UrlFormulaDescriptor(
						new URL("platform:/plugin/" + bundleId + "/formulas/" + getClass().getSimpleName() + ".svg"));
			} catch (MalformedURLException e) {}
		}
		return null;
	}
	
	
	@Override
	public FormulaDescriptor getFormulaDescriptor() {
		return formulaDescr;
	}
	
	
	@Override
	public NormalizedGrid calculate(NormalizationKey key) throws NormalizationException {
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		double[][] grid = null;
		
		double[] controls = calculateControls(key);
		
		if (key.getFeature() instanceof Feature) {
			Feature feature = (Feature)key.getFeature();
			grid = new double[plate.getRows()][plate.getColumns()];
			if (feature.isNumeric()) {
				// Obtain the raw values for the feature and normalize them.
				PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
				for (Well well: plate.getWells()) {
					double rawValue = accessor.getNumericValue(well, feature, null);
					grid[well.getRow()-1][well.getColumn()-1] = normalizeValue(rawValue, controls);
				}
			}
		} else {
			Well well = SelectionUtils.getAsClass(key.getDataToNormalize(), Well.class);
			SubWellFeature feature = (SubWellFeature)key.getFeature();
			if (feature.isNumeric()) {
				float[] values = SubWellService.getInstance().getNumericData(well, feature);
				grid = new double[1][values.length];
				for (int i=0; i<values.length; i++) {
					grid[0][i] = normalizeValue(values[i], controls);
				}
			} else {
				grid = new double[0][0];
			}
		}

		return new NormalizedGrid(grid);
	}
	
	protected abstract double[] calculateControls(NormalizationKey key);
	protected abstract double normalizeValue(double rawValue, double[] controls);
}
