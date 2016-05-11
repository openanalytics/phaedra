package eu.openanalytics.phaedra.datacapture.montage.layout;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;


public class FieldLayout {

	private int rows;
	private int columns;
	private int startingFieldNr;
	
	private Map<Integer, Point> fieldPositions;
	
	public FieldLayout(int startingFieldNr) {
		this(startingFieldNr, 0, 0);
	}
	
	public FieldLayout(int startingFieldNr, int rows, int columns) {
		this.startingFieldNr = startingFieldNr;
		this.fieldPositions = new HashMap<Integer, Point>();
		this.rows = rows;
		this.columns = columns;
	}

	public void addFieldPosition(int fieldNr, int columnIndex, int rowIndex) {
		fieldPositions.put(fieldNr - startingFieldNr, new Point(columnIndex, rowIndex));
		rows = Math.max(rows, rowIndex+1);
		columns = Math.max(columns, columnIndex+1);
	}

	public int getFieldNr(int fieldIndex) {
		return fieldIndex + startingFieldNr;
	}
	
	public Point getFieldPosition(int fieldNr) {
		return fieldPositions.get(fieldNr - startingFieldNr);
	}

	public String getLayoutString() {
		// Note: these field values are always zero-based. See Montage.java
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int r=0; r<rows; r++) {
			for (int c=0; c<columns; c++) {
				int fieldIndex = -1;
				for (int i: fieldPositions.keySet()) {
					Point pos = fieldPositions.get(i);
					if (pos.x == c && pos.y == r) {
						fieldIndex = i;
						break;
					}
				}
				sb.append(fieldIndex + ",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(";");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getColumns() {
		return columns;
	}
}
