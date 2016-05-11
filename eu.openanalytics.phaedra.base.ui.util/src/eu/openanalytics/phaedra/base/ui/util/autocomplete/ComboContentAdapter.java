package eu.openanalytics.phaedra.base.ui.util.autocomplete;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

public class ComboContentAdapter extends org.eclipse.jface.fieldassist.ComboContentAdapter {

	@Override
	public void setControlContents(Control control, String text, int cursorPosition) {
		super.setControlContents(control, text, cursorPosition);
		String[] items = ((Combo) control).getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].equalsIgnoreCase(text)) {
				((Combo) control).select(i);
				break;
			}
		}
		((Combo) control).notifyListeners(SWT.Selection, new Event());
	}

}