package eu.openanalytics.phaedra.ui.protocol.util;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IMessageManager;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class ListenerHelper {

	public static ModifyListener createTextNotEmptyListener(final String key, final IMessageManager mgr) {
		ModifyListener textNotEmptyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Text txt = (Text) e.getSource();
	            String value = txt.getText();

	            if ("".equals(value.trim())) {
	               mgr.addMessage(key, "This field cannot be empty", null, IMessageProvider.ERROR, txt);
	            } else {
	               mgr.removeMessage(key, txt);
	            }
			}
		};
		
		return textNotEmptyListener;
	}
	
	public static ModifyListener createTextNumericListener(final String key, final IMessageManager mgr) {
		ModifyListener textNotEmptyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				Text txt = (Text) e.getSource();
	            String value = txt.getText();
	            if (!NumberUtils.isDouble(value)) {
	               mgr.addMessage(key, "This field must be numeric", null, IMessageProvider.ERROR, txt);
	            } else {
	               mgr.removeMessage(key, txt);
	            }
			}
		};
		
		return textNotEmptyListener;
	}
	
}
