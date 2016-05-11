package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class ReflectingLabelProvider extends RichLabelProvider {

	private String methodName;
	
	public ReflectingLabelProvider(String method, ColumnConfiguration config) {
		super(config);
		methodName = method;
	}

	@Override
	public String getText(Object element) {
		Object resolvedElement = ReflectionUtils.invoke(methodName, element);
		return super.getText(resolvedElement);
	}
}
