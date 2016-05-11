package eu.openanalytics.phaedra.ui.curve.grid;

public class GridColumnGroup {

	private String name;
	private int[] columns;
	
	public GridColumnGroup(String name, int[] columns) {
		this.name = name;
		this.columns = columns;
	}
	
	public String getGroupName() {
		return name;
	}
	
	public int[] getGroupColumns() {
		return columns;
	}
}
