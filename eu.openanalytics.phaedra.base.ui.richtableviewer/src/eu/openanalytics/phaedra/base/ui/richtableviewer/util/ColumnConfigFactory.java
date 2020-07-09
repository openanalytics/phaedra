package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import java.util.function.Supplier;

import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.model.EntityProperty;
import eu.openanalytics.phaedra.base.model.EntityPropertyValueComparator;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.util.viewer.PropertyColumnLabelProvider;

public class ColumnConfigFactory {

	public static ColumnConfiguration create(String name, String getterName, DataType type, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		if (getterName != null) {
			config.setLabelProvider(new ReflectingLabelProvider(getterName, config));
			config.setSortComparator(new ReflectingColumnSorter(getterName));
		}
		return config;
	}
	
	public static ColumnConfiguration create(String name, DataType type, int width) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		return config;
	}
	
	public static ColumnConfiguration create(String name, DataType type, int width, float aspectRatio) {
		String key = name.toLowerCase().replace(' ', '_');
		ColumnConfiguration config = new ColumnConfiguration(key, name);
		config.setDataType(type);
		config.setWidth(width);
		config.setAspectRatio(aspectRatio);
		return config;
	}
	
	
	public static <T> ColumnConfiguration create(final EntityProperty<T> objectProperty, final Supplier<DataFormatter> dataFormatSupplier,
			final String name, final int width) {
		final DataDescription dataDescription = objectProperty.getDataDescription();
		final ColumnConfiguration config = new ColumnConfiguration(objectProperty.getKey(), objectProperty.getLabel());
		config.setTooltip(objectProperty.getLabel());
		config.setDataDescription(dataDescription);
		config.setWidth(width);
		config.setLabelProvider(new PropertyColumnLabelProvider<>(objectProperty, dataFormatSupplier));
		config.setSortComparator(new EntityPropertyValueComparator<>(objectProperty));
		return config;
	}
	
	public static <T> ColumnConfiguration create(final EntityProperty<T> objectProperty, final Supplier<DataFormatter> dataFormatSupplier,
			final int width) {
		return create(objectProperty, dataFormatSupplier, objectProperty.getShortLabel(), width);
	}
	
	
	public static ColumnConfiguration create(String name, ISimpleTextLabelProvider textProvider, DataType type, int width) {
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
		config.setSortComparator((e1,e2) -> {
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
		config.setDataType(DataType.Image);
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
		config.setLabelProvider(new ReflectingLabelProvider(getterName, config, "dd/MM/yyyy HH:mm:ss"));
	}
	
	public static interface ISimpleTextLabelProvider {
		public String getText(Object object);
	}
	
	public static interface ISimpleImageLabelProvider {
		public Image getImage(Object object);
	}

}
