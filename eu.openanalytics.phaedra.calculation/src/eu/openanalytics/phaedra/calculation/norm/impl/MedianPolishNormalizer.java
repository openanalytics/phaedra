package eu.openanalytics.phaedra.calculation.norm.impl;

import java.util.Arrays;

import org.apache.commons.math3.stat.StatUtils;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationKey;
import eu.openanalytics.phaedra.calculation.norm.cache.NormalizedGrid;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;


public class MedianPolishNormalizer extends BaseNormalizer {
	
	
	public MedianPolishNormalizer() {
	}
	
	
	@Override
	public String getId() {
		return "MedianPolish";
	}
	
	@Override
	public String getDescription() {
		return "Median polish normalization";
	}
	
	@Override
	public NormalizedGrid calculate(NormalizationKey key) throws NormalizationException {
		Plate plate = SelectionUtils.getAsClass(key.getDataToNormalize(), Plate.class);
		double[][] grid = null;
		
		if (key.getFeature() instanceof Feature) {
			Feature feature = (Feature)key.getFeature();
			grid = new double[plate.getRows()][plate.getColumns()];
			if (feature.isNumeric()) {
				PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
				double[][] eRows = new double[plate.getRows()][plate.getRows()];
				double[][] eCols = new double[plate.getColumns()][plate.getRows()];
				for (Well well: plate.getWells()) {
					double rawValue = accessor.getNumericValue(well, feature, null);
					grid[well.getRow()-1][well.getColumn()-1] = rawValue;
					eRows[well.getRow()-1][well.getColumn()-1] = (PlateUtils.ACCEPTED_WELLS_ONLY.test(well)) ?
							rawValue : Double.NaN;
				}
				
				int maxIter = 10;
				double eSum = Double.NaN;
				boolean converged = false;
				for (int i = 0; !converged && i < maxIter; i++) {
					double ePrevSum = eSum;
					eSum = 0;
					for (int iRow = 0; iRow < eRows.length; iRow++) {
						double median = StatUtils.percentile(eRows[iRow], 50.0);
						for (int iCol = 0; iCol < eCols.length; iCol++) {
							eCols[iCol][iRow] = eRows[iRow][iCol] - median;
						}
					}
					for (int iCol = 0; iCol < eCols.length; iCol++) {
						double median = StatUtils.percentile(eCols[iCol], 50.0);
						for (int iRow = 0; iRow < eRows.length; iRow++) {
							double v = eCols[iCol][iRow];
							if (!Double.isNaN(v)) {
								v -= median;
								eRows[iRow][iCol] = v;
								eSum += Math.abs(v);
							}
						}
					}
					double diff;
					converged = (eSum <= Double.MIN_NORMAL
							|| ((diff= ePrevSum - eSum) >= 0 && diff / ePrevSum < 0.01) );
				}
				
//				if (!converged) ?
				
				for (int iRow = 0; iRow < eRows.length; iRow++) {
					for (int iCol = 0; iCol < eCols.length; iCol++) {
						grid[iRow][iCol] -= eRows[iRow][iCol];
					}
				}
			} else {
				for (int iRow = 0; iRow < grid.length; iRow++) Arrays.fill(grid[iRow], Double.NaN);
			}
		} else {
			throw new NormalizationException("Not implemented for SubWellFeature");
//			Well well = SelectionUtils.getAsClass(key.getDataToNormalize(), Well.class);
//			SubWellFeature feature = (SubWellFeature)key.getFeature();
//			if (feature.isNumeric()) {
//				float[] values = SubWellService.getInstance().getNumericData(well, feature);
//				grid = new double[1][values.length];
				
//				for (int i=0; i<values.length; i++) {
//					grid[0][i] = normalizeValue(values[i], controls);
//				}
//			} else {
//				grid = new double[0][0];
//			}
		}
		return new NormalizedGrid(grid);
	}
	
	
	@Override
	protected double normalizeValue(double rawValue, double[] controls) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected double[] calculateControls(NormalizationKey key) {
		throw new UnsupportedOperationException();
	}
	
}
