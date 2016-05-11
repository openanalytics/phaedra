package eu.openanalytics.phaedra.calculation.norm.cache;

import java.io.Serializable;

public class NormalizedGrid implements Serializable {

	private static final long serialVersionUID = -4625187382494886024L;
	
	private double[][] grid;
	
	public NormalizedGrid(double[][] grid) {
		this.grid = grid;
	}
	
	public double getValue(int row, int col) {
		return grid[row-1][col-1];
	}
}
