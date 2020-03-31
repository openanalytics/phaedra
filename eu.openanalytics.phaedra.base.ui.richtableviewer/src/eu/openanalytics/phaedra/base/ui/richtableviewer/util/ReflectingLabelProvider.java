package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class ReflectingLabelProvider extends RichLabelProvider {

	private String methodName;
	
	public ReflectingLabelProvider(String method, ColumnConfiguration config, String formatString) {
		super(config, formatString);
		methodName = method;
	}
	
	public ReflectingLabelProvider(String method, ColumnConfiguration config) {
		this(method, config, null);
	}

	@Override
	public String getText(Object element) {
		Object resolvedElement = ReflectionUtils.invoke(methodName, element);
		return super.getText(resolvedElement);
	}
}
