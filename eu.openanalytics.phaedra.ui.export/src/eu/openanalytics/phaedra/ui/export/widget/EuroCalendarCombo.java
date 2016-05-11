package eu.openanalytics.phaedra.ui.export.widget;

import org.eclipse.nebula.widgets.calendarcombo.CalendarCombo;
import org.eclipse.nebula.widgets.calendarcombo.DefaultSettings;
import org.eclipse.nebula.widgets.calendarcombo.ISettings;
import org.eclipse.swt.widgets.Composite;

public class EuroCalendarCombo extends CalendarCombo {

	private final static String FORMAT = "dd/MM/yyyy";
	
	private final static ISettings CUSTOM_SETTINGS = new DefaultSettings(){
		public String getDateFormat() {
			return FORMAT;
		};
	};
	
	public EuroCalendarCombo(Composite parent, int style) {
		super(parent, style, CUSTOM_SETTINGS, null);
	}

}
