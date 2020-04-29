package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataComparator;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class MultiplateWellTableColumns {
	
	
	public static ColumnConfiguration[] configureColumns(final AsyncDataLoader<Plate> dataLoader,
			final List<Feature> features,
			final Supplier<DataFormatter> dataFormatSupplier, final Runnable refreshViewer) {
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Plate", DataType.String, 50);
		RichLabelProvider labelProviderPlate = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well)element;
				return well.getPlate().getBarcode();
			}
		};
		config.setLabelProvider(labelProviderPlate);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Row, dataFormatSupplier, 50);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Column, dataFormatSupplier, 50);
		configs.add(config);
		
		config = ColumnConfigFactory.create(WellProperty.WellType, dataFormatSupplier, 75);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Status", "getStatus", DataType.Integer, 75);
		configs.add(config);

		config = ColumnConfigFactory.create(WellProperty.Compound, dataFormatSupplier, 100);
		config.setSortComparator(PlateUtils.WELL_COMPOUND_NR_SORTER);
		configs.add(config);
		
		config = ColumnConfigFactory.create(WellProperty.Concentration, dataFormatSupplier, 90);
		configs.add(config);
		
		/* Feature columns*/
		final AsyncDataLoader<Plate>.DataAccessor<PlateDataAccessor> featureDataAccessor = dataLoader.addDataRequest((plate) -> {
			final PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
			accessor.loadEager(null);
			return accessor;
		});
		for (final Feature f: features) {
			config = ColumnConfigFactory.create(f.getDisplayName(), f.isNumeric() ? DataType.Real : DataType.String, 90);
			WellFeatureLabelProvider labelProvider = new WellFeatureLabelProvider(featureDataAccessor, f, refreshViewer);
			config.setLabelProvider(labelProvider);
			config.setSortComparator(new WellFeatureComparator(featureDataAccessor, f));
			config.setTooltip(f.getDisplayName());
			configs.add(config);
		}
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
	
	private static class WellFeatureComparator extends AsyncDataComparator<Plate, Well, PlateDataAccessor> {
		
		private final Feature feature;
		
		public WellFeatureComparator(final AsyncDataLoader<Plate>.DataAccessor<PlateDataAccessor> dataAccessor,
				final Feature feature) {
			super(dataAccessor);
			this.feature = feature;
		}
		
		@Override
		protected Object getData(final Well element) {
			return this.dataAccessor.getData(element.getPlate());
		}
		
		@Override
		protected int compareData(final Well element1, final PlateDataAccessor data1, final Well element2, final PlateDataAccessor data2) {
			if (this.feature.isNumeric()) {
				double v1 = data1.getNumericValue(element1, this.feature, this.feature.getNormalization());
				double v2 = data2.getNumericValue(element2, this.feature, this.feature.getNormalization());
				if (Double.isNaN(v1) && Double.isNaN(v2)) return 0;
				if (Double.isNaN(v1)) return -1;
				if (Double.isNaN(v2)) return 1;
				return Double.compare(v1, v2);
			} else {
				String s1 = data1.getStringValue(element1, this.feature);
				String s2 = data2.getStringValue(element2, this.feature);
				if (s1 == null) return 1;
				return s1.compareTo(s2);
			}
		}
		
	}
	
	private static class WellFeatureLabelProvider extends AsyncDataLabelProvider<Plate, Well, PlateDataAccessor> {
		
		private final Feature feature;
		
		private ColorStore colorStore;
		private boolean useColors = true;
		private IPropertyChangeListener prefListener;
		
		private IColorMethod colorMethod;
		
		public WellFeatureLabelProvider(final AsyncDataLoader<Plate>.DataAccessor<PlateDataAccessor> dataAccessor,
				final Feature feature, final Runnable refreshViewer) {
			super(dataAccessor);
			this.feature = feature;
			
			this.useColors = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.WELL_TABLE_COLORS);
			this.colorStore = new ColorStore();
			
			this.prefListener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(Prefs.WELL_TABLE_COLORS)) {
						useColors = (Boolean)event.getNewValue();
						Display.getDefault().asyncExec(refreshViewer);
					}
				}
			};
			Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		}
		
		private RGB getColor(final PlateDataAccessor data, final double value) {
			IColorMethod colorMethod = this.colorMethod;
			if (colorMethod == null) {
				colorMethod = ColorMethodFactory.createColorMethod(this.feature);
				colorMethod.initialize(ColorMethodFactory.createData(data, this.feature, this.feature.getNormalization()));
				this.colorMethod = colorMethod;
			}
			return colorMethod.getColor(value);
		}
		
		@Override
		protected Object getData(final Well element) {
			return this.dataAccessor.getData(element.getPlate());
		}
		
		@Override
		protected String getText(final Well element, final PlateDataAccessor data) {
			if (feature.isNumeric()) {
				double value = data.getNumericValue(element, this.feature, this.feature.getNormalization());
				return Formatters.getInstance().format(value, this.feature);
			} else {
				return data.getStringValue(element, this.feature);
			}
		}
		
		@Override
		public Color getForeground(final Well element, final PlateDataAccessor data) {
			if (this.useColors && this.feature.isNumeric()) {
				double value = data.getNumericValue(element, this.feature, this.feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = getColor(data, value);
				return ColorUtils.getTextColor(rgb);
			}
			return null;
		}
		
		@Override
		public Color getBackground(final Well element, final PlateDataAccessor data) {
			if (this.useColors && this.feature.isNumeric()) {
				double value = data.getNumericValue(element, this.feature, this.feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = getColor(data, value);
				if (rgb != null) return this.colorStore.get(rgb);
			}
			return null;
		}
		
		@Override
		public void dispose() {
			Activator.getDefault().getPreferenceStore().removePropertyChangeListener(this.prefListener);
			this.colorStore.dispose();
			super.dispose();
		}
		
	}
	
}
