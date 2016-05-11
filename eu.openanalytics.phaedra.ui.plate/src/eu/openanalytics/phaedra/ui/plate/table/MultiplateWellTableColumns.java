package eu.openanalytics.phaedra.ui.plate.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class MultiplateWellTableColumns {

	public static ColumnConfiguration[] configureColumns(final PlateDataAccessor dataAccessor, TableViewer tableViewer) {
		
		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Plate", ColumnDataType.String, 50);
		RichLabelProvider labelProviderPlate = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well)element;
				return well.getPlate().getBarcode();
			}
		};

		config.setLabelProvider(labelProviderPlate);
		configs.add(config);

		config = ColumnConfigFactory.create("Row", "getRow", ColumnDataType.Numeric, 50);
		configs.add(config);

		config = ColumnConfigFactory.create("Column", "getColumn", ColumnDataType.Numeric, 50);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Well Type", "getWellType", ColumnDataType.String, 75);
		configs.add(config);
		
		config = ColumnConfigFactory.create("Status", "getStatus", ColumnDataType.Numeric, 75);
		configs.add(config);

		config = ColumnConfigFactory.create("Compound", ColumnDataType.String, 100);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				Well well = (Well)element;
				Compound c = well.getCompound();
				if (c != null) {
					return c.getType() + " " + c.getNumber();
				}
				return "";
			}
		};
		config.setSorter(PlateUtils.WELL_COMPOUND_NR_SORTER);
		config.setLabelProvider(labelProvider);
		config.setTooltip("Compound");
		configs.add(config);
		
		config = ColumnConfigFactory.create("Concentration", "getCompoundConcentration", ColumnDataType.Numeric, 90);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public void update(ViewerCell cell) {
				Well well = (Well)cell.getElement();
				double conc = well.getCompoundConcentration();
				cell.setText(String.valueOf(conc));
			}
		});
		configs.add(config);
		
		/* Feature columns*/
		
		List<Feature> features = dataAccessor.getPlate().getExperiment().getProtocol().getProtocolClass().getFeatures();
		

		for (final Feature f: features) {
			config = ColumnConfigFactory.create(f.getDisplayName(), ColumnDataType.Numeric, 90);
			labelProvider = new WellFeatureLabelProvider(config, f, dataAccessor, tableViewer);
			config.setLabelProvider(labelProvider);
			config.setSorter(new Comparator<Well>() {
				@Override
				public int compare(Well w1, Well w2) {
					PlateDataAccessor dataAccessor1 = CalculationService.getInstance().getAccessor(w1.getPlate());
					PlateDataAccessor dataAccessor2 = CalculationService.getInstance().getAccessor(w2.getPlate());
					if (f.isNumeric()) {
						double v1 = dataAccessor1.getNumericValue(w1, f, f.getNormalization());
						double v2 = dataAccessor2.getNumericValue(w2, f, f.getNormalization());
						if (Double.isNaN(v1) && Double.isNaN(v2)) return 0;
						if (Double.isNaN(v1)) return -1;
						if (Double.isNaN(v2)) return 1;
						if (v1 > v2) return 1;
						if (v1 == v2) return 0;
						return -1;
					} else {
						String s1 = dataAccessor1.getStringValue(w1,f);
						String s2 = dataAccessor2.getStringValue(w2,f);
						if (s1 == null) return 1;
						return s1.compareTo(s2);
					}
				}
			});
			config.setTooltip(f.getDisplayName());
			configs.add(config);
		}
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
	
	private static class WellFeatureLabelProvider extends RichLabelProvider {
			
		private Feature feature;
		private IColorMethod colorMethod;
		private ColorStore colorStore;
		private boolean useColors = true;
		private IPropertyChangeListener prefListener;
		
		public WellFeatureLabelProvider(ColumnConfiguration config,
				Feature feature, PlateDataAccessor dataAccessor, final TableViewer tableViewer) {
			super(config);
			
			this.feature = feature;
			useColors = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.WELL_TABLE_COLORS);
			
			colorMethod = ColorMethodFactory.createColorMethod(feature);
			colorMethod.initialize(ColorMethodFactory.createData(dataAccessor, feature, feature.getNormalization()));
			colorStore = new ColorStore();
			
			prefListener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(Prefs.WELL_TABLE_COLORS)) {
						useColors = (Boolean)event.getNewValue();
						tableViewer.refresh();
					}
				}
			};
			Activator.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
		}
		
		@Override
		public String getText(Object element) {
			Well well = (Well)element;
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			if (feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				return Formatters.getInstance().format(value, feature);
			} else {
				return dataAccessor.getStringValue(well, feature);
			}
		}
		
		@Override
		public Color getForeground(Object element) {
			Well well = (Well)element;
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			if (useColors && feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = colorMethod.getColor(value);
				return ColorUtils.getTextColor(rgb);
			}
			
			return null;
		}
		
		@Override
		public Color getBackground(Object element) {
			Well well = (Well)element;
			PlateDataAccessor dataAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
			if (useColors && feature.isNumeric()) {
				double value = dataAccessor.getNumericValue(well, feature, feature.getNormalization());
				if (Double.isNaN(value)) return null;
				RGB rgb = colorMethod.getColor(value);
				if (rgb != null) return colorStore.get(rgb);
			}
			
			return null;
		}
		
		@Override
		public void dispose() {
			Activator.getDefault().getPreferenceStore().removePropertyChangeListener(prefListener);
			colorStore.dispose();
			super.dispose();
		}
	}
}
