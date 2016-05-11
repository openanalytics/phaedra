package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class FlagLabelProvider extends RichLabelProvider {
	
	public static enum FlagFilter {
		Negative,
		NegativeOrZero,
		Zero,
		One,
		GreaterThanOne,
		Positive,
		PositiveOrZero,
		All
	}
	
	private FlagMapping[] mappings;
	private String methodName;
	
	public FlagLabelProvider(ColumnConfiguration config, String methodName, FlagMapping... mappings) {
		super(config);
		this.methodName = methodName;
		this.mappings = mappings;
	}
	
	@Override
	public Image getImage(Object element) {
		Object retVal = ReflectionUtils.invoke(methodName, element);
		int value = 0;
		if (retVal instanceof Boolean) {
			value = (Boolean)retVal ? 1 : 0;
		} else if (retVal instanceof Integer) {
			value = (Integer)retVal;
		}
		
		for (FlagMapping mapping: mappings) {
			if (mapping.matches(value)) return IconManager.getIconImage(mapping.flagIcon);
		}
		return null;
	}
	
	@Override
	public String getText(Object element) {
		return null;
	}
	
	public static class FlagMapping {
		public FlagFilter filter;
		public String flagIcon;
		
		public FlagMapping(FlagFilter filter, String icon) {
			this.filter = filter;
			this.flagIcon = icon;
		}
		
		public boolean matches(int value) {
			switch (filter) {
			case Negative: return value < 0;
			case NegativeOrZero: return value <= 0;
			case Zero: return value == 0;
			case One: return value == 1;
			case GreaterThanOne: return value > 1;
			case Positive: return value > 0;
			case PositiveOrZero: return value >= 0;
			case All: return true;
			}
			return false;
		}
	}
}
