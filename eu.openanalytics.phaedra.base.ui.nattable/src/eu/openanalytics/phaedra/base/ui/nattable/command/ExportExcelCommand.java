package eu.openanalytics.phaedra.base.ui.nattable.command;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.AbstractContextFreeCommand;

public class ExportExcelCommand extends AbstractContextFreeCommand {

	private NatTable natTable;
	
	public ExportExcelCommand(NatTable natTable) {
		this.natTable = natTable;
	}
	
	public NatTable getNatTable() {
		return natTable;
	}

}
