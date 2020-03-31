package eu.openanalytics.phaedra.base.ui.richtableviewer;

import java.text.Format;
import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class RichLabelProvider extends ColumnLabelProvider {

	private ColumnConfiguration config;
	private Format formatter;
	
	private final static String DEFAULT_DECIMAL_FORMAT = NumberUtils.DEFAULT_DECIMAL_FORMAT;
	private final static String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
	
	
	public RichLabelProvider(ColumnConfiguration config, String formatString) {
		this.config = config;
		formatter = createFormatter(formatString);
	}
	
	public RichLabelProvider(ColumnConfiguration config) {
		this(config, null);
	}
	
	
	@Override
	public String getText(Object element) {
		if (element == null) return null;
		if (formatter != null) {
			return formatter.format(element);
		}
		return element.toString();
	}
	
	private Format createFormatter(String formatString) {
		
		switch (config.getDataType()) {
		case Integer:
		case Real:
			if (formatString == null) formatString = DEFAULT_DECIMAL_FORMAT;
			return NumberUtils.createDecimalFormat(formatString);
		case String:
			return null;
		case DateTime:
			if (formatString == null) formatString = DEFAULT_DATE_FORMAT;
			return new SimpleDateFormat(formatString);
		case Boolean:
			return null;
		default:
			return null;
		}
	}
}
