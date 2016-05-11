package eu.openanalytics.phaedra.calculation.stat.filter;

public class NaNFilter implements IFilter {

	@Override
	public double[] apply(double[] values) {
		if (values == null || values.length == 0) return new double[0];
		if (values.length == 1 && Double.isNaN(values[0])) return new double[0];
		if (values.length == 1 && !Double.isNaN(values[0])) return values;
		
		double[] output = new double[values.length];
		int index = 0;
		for (int i=0; i<values.length; i++) {
			if (!Double.isNaN(values[i])) output[index++] = values[i];
		}
		if (index == values.length) return values; // No NaNs found: no need to copy arrays.
		double[] filteredOutput = new double[index];
		System.arraycopy(output, 0, filteredOutput, 0, index);
		return filteredOutput;
	}

}
