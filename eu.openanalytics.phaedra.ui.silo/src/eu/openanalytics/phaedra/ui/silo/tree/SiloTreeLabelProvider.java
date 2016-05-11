package eu.openanalytics.phaedra.ui.silo.tree;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.silo.util.SiloStructure;

public class SiloTreeLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		SiloStructure struct = (SiloStructure)cell.getElement();
		
		String baseText = struct.getName();
		String decoration = null;
		Styler decorationStyler = null;
		if (!struct.isDataset() && !struct.getChildren().isEmpty()) {
			decoration = "[" + struct.getChildren().get(0).getDatasetSize() + "x" + struct.getChildren().size() + "]";
			decorationStyler = StyledString.COUNTER_STYLER;
		}
		
		StyledString styledString = new StyledString(baseText, null);
		if (decoration != null) {
			styledString.append(" " + decoration, decorationStyler);				
		}
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
		
		if (!struct.getChildren().isEmpty()) {
			cell.setImage(IconManager.getIconImage("dataframe.png"));
		} else {
			cell.setImage(IconManager.getIconImage("dataframe_col.png"));
		}
	}
}
