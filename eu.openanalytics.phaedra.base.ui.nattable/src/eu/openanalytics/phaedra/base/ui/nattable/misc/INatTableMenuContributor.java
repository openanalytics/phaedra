package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Menu;

public interface INatTableMenuContributor {

	public void fillMenu(NatTable table, Menu menu);

}
