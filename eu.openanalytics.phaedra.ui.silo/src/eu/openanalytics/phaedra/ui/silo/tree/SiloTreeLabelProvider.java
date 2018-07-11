package eu.openanalytics.phaedra.ui.silo.tree;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class SiloTreeLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		String baseText = null;
		String decoration = null;
		Styler decorationStyler = null;
		
		if (cell.getElement() instanceof Silo) {
			Silo silo = (Silo) cell.getElement();
			baseText = silo.getName();
			
			int dsCount = silo.getDatasets().size();
			if (dsCount > 0) {
				decoration = "[" + dsCount + " datasets]";
				decorationStyler = StyledString.COUNTER_STYLER;	
			}
		} else if (cell.getElement() instanceof SiloDataset) {
			SiloDataset ds = (SiloDataset) cell.getElement();
			baseText = ds.getName();
			
			ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(ds.getSilo());
			try {
				int rowCount = accessor.getRowCount(ds.getName());
				if (rowCount == 0) {
					decoration = "[empty]";
					decorationStyler = StyledString.COUNTER_STYLER;
				} else {
					decoration = "[" + rowCount + " rows]";
					decorationStyler = StyledString.COUNTER_STYLER;	
				}
			} catch (SiloException e) {
				EclipseLog.error("Failed to load silo data", e, Activator.PLUGIN_ID);
			}
		}
		
		StyledString styledString = new StyledString(baseText, null);
		if (decoration != null) styledString.append(" " + decoration, decorationStyler);				
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setImage(IconManager.getIconImage("dataframe.png"));
	}
}
