package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseClickAction;
import org.eclipse.swt.events.MouseEvent;

public class DefaultMouseClickAction implements IMouseClickAction {

	@Override
	public void run(NatTable table, MouseEvent event) {
		// Default: do nothing
	}

	@Override
	public boolean isExclusive() {
		return false;
	}

}
