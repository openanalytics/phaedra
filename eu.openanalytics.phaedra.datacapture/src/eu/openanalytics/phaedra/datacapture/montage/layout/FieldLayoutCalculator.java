package eu.openanalytics.phaedra.datacapture.montage.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FieldLayoutCalculator {

	private int startingFieldNr;
	private Set<FieldRef> fields;
	
	public FieldLayoutCalculator(int startingFieldNr) {
		this.startingFieldNr = startingFieldNr;
		this.fields = new HashSet<>();
	}
	
	public void addField(double x, double y, int fieldNr) {
		fields.add(new FieldRef(fieldNr, x, y));
	}
	
	public FieldLayout calculate() {
		FieldRef[] fieldArray = fields.toArray(new FieldRef[fields.size()]);
		Arrays.sort(fieldArray, new FieldYXSorter());
		
		Set<Double> xValues = new HashSet<>();
		Set<Double> yValues = new HashSet<>();
		for (FieldRef field: fieldArray) {
			xValues.add(field.x);
			yValues.add(field.y);
		}
		List<Double> sortedXValues = new ArrayList<>(xValues);
		List<Double> sortedYValues = new ArrayList<>(yValues);
		Collections.sort(sortedXValues);
		Collections.sort(sortedYValues);
		
		int columnCount = xValues.size();
		int rowCount = yValues.size();
		
		FieldLayout layout = new FieldLayout(startingFieldNr);
		
		for (int r=0; r<rowCount; r++) {
			double yValue = sortedYValues.get(r);
			for (int c=0; c<columnCount; c++) {
				double xValue = sortedXValues.get(c);
				
				FieldRef matchingField = null;
				for (FieldRef f: fieldArray) {
					if (f.x == xValue && f.y == yValue) {
						matchingField = f;
						break;
					}
				}
				
				if (matchingField == null) layout.addFieldPosition(-1, c, r);
				else layout.addFieldPosition(matchingField.nr, c, r);
			}
		}
		
		return layout;
	}
	
	private static class FieldYXSorter implements Comparator<FieldRef> {
		@Override
		public int compare(FieldRef o1, FieldRef o2) {
			if (o1.y < o2.y) return -1;
			if (o1.y > o2.y) return 1;
			if (o1.x < o2.x) return -1;
			if (o1.x > o2.x) return 1;
			return 0;
		}
	}
	
	private static class FieldRef {
		public int nr;
		public double x;
		public double y;
		
		public FieldRef(int nr, double x, double y) {
			this.nr = nr;
			this.x = x;
			this.y = y;
		}
	}
}
