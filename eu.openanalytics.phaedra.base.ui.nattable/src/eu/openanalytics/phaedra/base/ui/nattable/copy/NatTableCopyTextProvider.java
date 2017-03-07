package eu.openanalytics.phaedra.base.ui.nattable.copy;

import java.util.Collection;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataToClipboardCommand;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;

import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.copy.extension.ICopyTextProvider;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class NatTableCopyTextProvider implements ICopyTextProvider {

	@Override
	public boolean isValidWidget(Object widget) {
		return widget instanceof NatTable;
	}

	@Override
	public Object getValueToCopy(Object widget) {
		if (isValidWidget(widget)) {
			NatTable table = (NatTable) widget;
			
			GridLayer gridLayer = (GridLayer) table.getLayer();
			SelectionLayer selectionLayer = ReflectionUtils.getFieldObject(gridLayer.getBodyLayer(), "selectionLayer", SelectionLayer.class);
			Collection<ILayerCell> cells = selectionLayer.getSelectedCells();
			
			if (cells.size() == 1) {
				return cells.iterator().next().getDataValue();
			} else {
				table.doCommand(new CopyDataToClipboardCommand("\t", System.getProperty("line.separator"), table.getConfigRegistry()));
				return CopyItems.getCurrentTextFromClipboard();
			}
		}
		return null;
	}


}
