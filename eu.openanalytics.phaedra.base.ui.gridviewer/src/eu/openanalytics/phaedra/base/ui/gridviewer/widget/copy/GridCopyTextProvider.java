package eu.openanalytics.phaedra.base.ui.gridviewer.widget.copy;

import java.util.Set;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.util.copy.extension.ICopyTextProvider;

public class GridCopyTextProvider implements ICopyTextProvider {

	@Override
	public boolean isValidWidget(Object widget) {
		return widget instanceof Grid;
	}

	@Override
	public Object getValueToCopy(Object widget) {
		String output = "";
		if (isValidWidget(widget)) {
			Grid grid = (Grid) widget;
			output = getTextToCopy(grid);
		}
		return output;
	}

	private String getTextToCopy(Grid grid) {
		StringBuilder sb = new StringBuilder();

		Set<GridCell> selectedCells = grid.getSelectedCells();
		if (!selectedCells.isEmpty()) {
			int colStart = grid.getColumns();
			int rowStart = grid.getRows();
			int colEnd = 0;
			int rowEnd = 0;
			for (GridCell cell : selectedCells) {
				int col = cell.getColumn();
				int row = cell.getRow();

				colStart = Math.min(col, colStart);
				rowStart = Math.min(row, rowStart);
				colEnd = Math.max(col, colEnd);
				rowEnd = Math.max(row, rowEnd);
			}
			colEnd++;
			rowEnd++;

			GridCell[][] cells = new GridCell[rowEnd-rowStart][colEnd-colStart];
			for (GridCell cell : selectedCells) {
				int col = cell.getColumn() - colStart;
				int row = cell.getRow() - rowStart;
				cells[row][col] = cell;
			}

			String cellDelimeter = "\t";
			String rowDelimeter = System.getProperty("line.separator");
			for (int row = 0; row < cells.length; row++) {
				for (int col = 0; col < cells[row].length; col++) {
					GridCell cell = cells[row][col];
					if (cell != null) {
						sb.append(cell.getTooltip().replace("\n", " "));
					} else {
						sb.append(cellDelimeter);
					}
					sb.append(cellDelimeter);
				}
				sb.append(rowDelimeter);
			}
		}

		return sb.toString();
	}

}
