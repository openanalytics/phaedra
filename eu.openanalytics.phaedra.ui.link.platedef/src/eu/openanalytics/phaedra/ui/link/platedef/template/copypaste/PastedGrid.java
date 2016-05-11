package eu.openanalytics.phaedra.ui.link.platedef.template.copypaste;

public class PastedGrid {

	private int rows;
	private int columns;
	private String[][] data;
	
	public PastedGrid(String text) {
		String[] rows = text.split("\r\n");
		this.rows = rows.length;
		this.data = new String[this.rows][];
		for (int i = 0; i < rows.length; i++) {
			String[] cols = rows[i].split("\t");
			this.columns = Math.max(this.columns, cols.length);
		}
		
		for (int i = 0; i < rows.length; i++) {
			this.data[i] = new String[this.columns];
			String[] cols = rows[i].split("\t");
			for (int j = 0; j < cols.length; j++) {
				this.data[i][j] = cols[j].trim();
			}
		}
	}
	
	public String get(int r, int c) {
		return data[r-1][c-1];
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getColumns() {
		return columns;
	}
}
