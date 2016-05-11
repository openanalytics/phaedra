package eu.openanalytics.phaedra.base.ui.nattable.copy;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.copy.command.CopyDataToClipboardCommand;

import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.copy.extension.ICopyTextProvider;

public class NatTableCopyTextProvider implements ICopyTextProvider {

	@Override
	public boolean isValidWidget(Object widget) {
		return widget instanceof NatTable;
	}

	@Override
	public String getTextToCopy(Object widget) {
		String output = "";
		if (isValidWidget(widget)) {
			NatTable table = (NatTable) widget;
			output = getTextToCopy(table);
		}
		return output;
	}

	private String getTextToCopy(NatTable table) {
		String cellDelimeter = "\t";
		String rowDelimeter = System.getProperty("line.separator");
	    IConfigRegistry configRegistry = table.getConfigRegistry();

	    table.doCommand(new CopyDataToClipboardCommand(cellDelimeter, rowDelimeter, configRegistry));

	    return CopyItems.getCurrentTextFromClipboard();
	}

}
