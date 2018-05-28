package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.DefaultComparator;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Strings;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.plate.vo.FeatureValue;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseWells;

public class FeatureValueQueryEditorSupport extends AbstractQueryEditorSupport {
	
	@Override
	public Class<?> getSupportedClass() {
		return FeatureValue.class;
	}

	@Override
	public String getLabel() {
		return "Well Feature Value";
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new FeatureValueColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				FeatureValue fv = SelectionUtils.getFirstObject(sel, FeatureValue.class);
				if (fv != null) BrowseWells.execute(fv.getWell());
			};
		});
	}

	private class FeatureValueColumnAccessor extends RichColumnAccessor<FeatureValue> {

		private String[] columns = { "Protocol", "Experiment", "Plate", "Well Nr", "Feature", "Normalisation", "Normalized", "Raw" };

		@Override
		public Object getDataValue(FeatureValue rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getWell().getPlate().getExperiment().getProtocol().getName();
			case 1:
				return rowObject.getWell().getPlate().getExperiment().getName();
			case 2:
				return rowObject.getWell().getPlate().getBarcode();
			case 3:
				return NumberUtils.getWellCoordinate(rowObject.getWell().getRow(), rowObject.getWell().getColumn());
			case 4:
				return rowObject.getFeature().getName();
			case 5:
				return rowObject.getFeature().getNormalization();
			case 6:
				return Formatters.getInstance().format(rowObject.getNormalizedValue(), rowObject.getFeature());
			case 7:
				if (Strings.isNullOrEmpty(rowObject.getRawStringValue())) {
					return Formatters.getInstance().format(rowObject.getRawNumericValue(), rowObject.getFeature());
				}
				return rowObject.getRawStringValue();
			default:
				break;
			}
			return null;
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnProperty(int columnIndex) {
			return columns[columnIndex];
		}

		@Override
		public int getColumnIndex(String propertyName) {
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].equals(propertyName)) return i;
			}
			return 0;
		}

		@Override
		public String getTooltipText(FeatureValue rowObject, int colIndex) {
			if (rowObject != null) {
				if (colIndex == 6) {
					double value = rowObject.getNormalizedValue();
					return value + "\n\n" + Formatters.getInstance().format(value, rowObject.getFeature());
				}
				if (colIndex == 7) {
					if (Strings.isNullOrEmpty(rowObject.getRawStringValue())) {
						double value = rowObject.getRawNumericValue();
						return value + "\n\n" + Formatters.getInstance().format(value, rowObject.getFeature());
					}
					return rowObject.getRawStringValue();
				}
			}
			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 200;
			columnWidths[1] = 200;
			columnWidths[3] = 50;
			columnWidths[4] = 200;
			columnWidths[6] = 150;
			columnWidths[7] = 150;
			return columnWidths;
		}

		@Override
		public IConfiguration getCustomConfiguration() {
			return new AbstractRegistryConfiguration() {
				@Override
				public void configureRegistry(IConfigRegistry configRegistry) {
					// Register custom comparators
					configRegistry.registerConfigAttribute(
							SortConfigAttributes.SORT_COMPARATOR
							, (String s1, String s2) -> StringUtils.compareToNumericStrings(s1, s2)
							, DisplayMode.NORMAL
							, columns[3]
					);
					configRegistry.registerConfigAttribute(
							SortConfigAttributes.SORT_COMPARATOR
							, (String v1, String v2) -> {
								return DefaultComparator.getInstance().compare(Double.valueOf(v1), Double.valueOf(v2));
							}
							, DisplayMode.NORMAL
							, columns[6]
					);
					configRegistry.registerConfigAttribute(
							SortConfigAttributes.SORT_COMPARATOR
							, (String v1, String v2) -> {
								if (NumberUtils.isDouble(v1) && NumberUtils.isDouble(v2)) {
									return DefaultComparator.getInstance().compare(Double.valueOf(v1), Double.valueOf(v2));
								} else {
									return v1.compareTo(v2);
								}
							}
							, DisplayMode.NORMAL
							, columns[7]
					);
				}
			};
		}
	};

}
