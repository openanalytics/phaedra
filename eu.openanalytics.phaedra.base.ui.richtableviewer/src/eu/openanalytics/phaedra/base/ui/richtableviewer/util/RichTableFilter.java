package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;

public class RichTableFilter extends ViewerFilter {

	private ColumnConfiguration[] columns;
	private String columnName;
	private StringMatcher matcher;

	public void setSearchText(String text) {
		if (text == null || text.isEmpty()) {
			this.matcher = null;
		} else {
			this.matcher = new StringMatcher(text, true, false);
		}
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public void setColumns(ColumnConfiguration[] columns) {
		this.columns = columns;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (matcher == null || columns == null || columnName == null) {
			return true;
		}

		ColumnConfiguration column = null;
		for (ColumnConfiguration col: columns) {
			if (col.getName().equals(columnName)) {
				column = col;
				break;
			}
		}
		if (column == null) return true;

		CellLabelProvider cellLabelProvider = column.getLabelProvider();
		if (!(cellLabelProvider instanceof ColumnLabelProvider)) return true;
		ColumnLabelProvider columnLabelProvider = (ColumnLabelProvider)cellLabelProvider;

		String text = columnLabelProvider.getText(element);
		return matcher.match(text);
	}

}
