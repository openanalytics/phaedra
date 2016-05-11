package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;

public class ColumnConfigFactory {

	public static ColumnConfiguration create(String name, String getterName, ColumnDataType type, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		if (getterName != null) {
			config.setLabelProvider(new ReflectingLabelProvider(getterName, config));
			config.setSorter(new ReflectingColumnSorter(getterName));
		}
		return config;
	}
	
	public static ColumnConfiguration create(String name, ColumnDataType type, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		return config;
	}
	
	public static ColumnConfiguration create(String name, ColumnDataType type, int width, float aspectRatio) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		config.setAspectRatio(aspectRatio);
		return config;
	}
	
	public static ColumnConfiguration create(String name, ISimpleTextLabelProvider textProvider, ColumnDataType type, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				return textProvider.getText(element);
			}
		});
		config.setSorter((e1,e2) -> {
			String s1 = textProvider.getText(e1);
			String s2 = textProvider.getText(e2);
			if (s1 == null && s2 == null) return 0;
			if (s1 == null) return -1;
			return s1.compareTo(s2);
		});
		return config;
	}
	
	public static ColumnConfiguration create(String name, ISimpleImageLabelProvider imageProvider, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(ColumnDataType.Image);
		config.setWidth(width);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				return null;
			}
			@Override
			public Image getImage(Object element) {
				return imageProvider.getImage(element);
			}
		});
		return config;
	}
	
	public static void createLabelProvider(ColumnConfiguration config, String getterName, String format) {
		config.setFormatString("dd/MM/yyyy HH:mm:ss");
		config.setLabelProvider(new ReflectingLabelProvider(getterName, config));
	}
	
	public static interface ISimpleTextLabelProvider {
		public String getText(Object object);
	}
	
	public static interface ISimpleImageLabelProvider {
		public Image getImage(Object object);
	}
}
