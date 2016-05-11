package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class TreeSearchFilter extends ViewerFilter {
	
	private String columnName;
	private String searchText;
	
	public void setSearchText(String text){
		this.searchText =  text;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
	
		if (searchText == null || searchText.isEmpty() || columnName == null) {
			return true;
		}

		TreeViewer treeViewer = (TreeViewer)viewer;
		
		int colIndex = 0;
		for (; colIndex < treeViewer.getTree().getColumnCount(); colIndex++) {
			if (treeViewer.getTree().getColumn(colIndex).getText().equals(columnName)) {
				break;
			}
		}
		if (colIndex >= treeViewer.getTree().getColumnCount()) return true;
		
		CellLabelProvider cellLabelProvider = treeViewer.getLabelProvider(colIndex);
		if (cellLabelProvider == null) return true;
		
		String text = null;
		if (cellLabelProvider instanceof DefaultCellLabelProvider) {
			text = ((DefaultCellLabelProvider)cellLabelProvider).getText(element);
		} else if (cellLabelProvider instanceof HyperlinkLabelProvider) {
			text = ((HyperlinkLabelProvider)cellLabelProvider).getText(element);
		}
		
		if (text == null) return true;
		return text.toLowerCase().contains(searchText.toLowerCase());
	}
}