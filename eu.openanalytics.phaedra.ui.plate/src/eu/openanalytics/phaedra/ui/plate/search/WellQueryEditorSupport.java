package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseWells;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

public class WellQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Well.class;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new WellColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Well well = SelectionUtils.getFirstObject(sel, Well.class);
				if (well != null) BrowseWells.execute(well);
			};
		});
	}

	private class WellColumnAccessor extends RichColumnAccessor<Well> {

		private String[] columns = { "Protocol", "Experiment", "Plate", "ID", "Well Nr", "Row", "Col"
				, "V", "Well Type", "Description", "Compound", "Concentration", "Saltform" };

		@Override
		public Object getDataValue(Well rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getPlate().getExperiment().getProtocol().getName();
			case 1:
				return rowObject.getPlate().getExperiment().getName();
			case 2:
				return rowObject.getPlate().getBarcode();
			case 3:
				return rowObject.getId();
			case 4:
				return NumberUtils.getWellCoordinate(rowObject.getRow(), rowObject.getColumn());
			case 5:
				return rowObject.getRow();
			case 6:
				return rowObject.getColumn();
			case 7:
				return rowObject.getStatus();
			case 8:
				return rowObject.getWellType();
			case 9:
				return rowObject.getDescription();
			case 10:
				Compound c0 = rowObject.getCompound();
				if (c0 == null) return null;
				return c0.getType() + " " + c0.getNumber();
			case 11:
				return rowObject.getCompoundConcentration();
			case 12:
				Compound c1 = rowObject.getCompound();
				if (c1 == null) return null;
				return c1.getSaltform();
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
		public String getTooltipText(Well rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 3) return "Well ID";
				if (colIndex == 6) return "Column";
				if (colIndex == 7) return "Well Validation Status";
				if (colIndex == 12) return "Saltform";
			} else {
				if (colIndex == 7) return WellStatus.getByCode(rowObject.getStatus()).getDescription();
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 150;
			columnWidths[1] = 150;
			columnWidths[3] = 50;
			columnWidths[4] = 50;
			columnWidths[5] = 35;
			columnWidths[6] = 35;
			columnWidths[7] = 35;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			int[] flagColumns = new int[] { 7 };
			painters.put(flagColumns, new FlagCellPainter(new FlagMapping(FlagFilter.Negative, Flag.Red),
					new FlagMapping(FlagFilter.Zero, Flag.White), new FlagMapping(FlagFilter.Positive, Flag.Green)));

			return painters;
		}

		@Override
		public IConfiguration getCustomConfiguration() {
			return new WellTableConfiguration();
		}

		private class WellTableConfiguration extends AbstractRegistryConfiguration {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				// Register custom comparator
				configRegistry.registerConfigAttribute(
						SortConfigAttributes.SORT_COMPARATOR
						, (String s1, String s2) -> StringUtils.compareToNumericStrings(s1, s2)
						, DisplayMode.NORMAL
						, columns[4]
				);

				applyStatusFilter(configRegistry, 6, WellStatus.class);
			}

			private <E extends Enum<E>> void applyStatusFilter(IConfigRegistry configRegistry, int column, Class<E> enumData) {
				Function<Object, String> wellStatusToString = new Function<Object, String>() {
					@Override
					public String apply(Object t) {
						if (t instanceof WellStatus) return ((WellStatus) t).getLabel();
						else return t.toString();
					}
				};
				Function<Object, String> wellStatusToCodeString = new Function<Object, String>() {
					@Override
					public String apply(Object t) {
						if (t instanceof EntityStatus) return ((EntityStatus) t).getCode()+"";
						else return t.toString();
					}
				};

				Function<Object, Object> comboConverter = new Function<Object, Object>() {
					@Override
					public Object apply(Object t) {
						if (t instanceof WellStatus) return ((WellStatus) t).getLabel();
						return t.toString();
					}
				};

				NatTableUtils.applyAdvancedComboFilter(configRegistry, column, Arrays.asList(enumData.getEnumConstants())
						, new FunctionDisplayConverter(comboConverter)
						, new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								// Manually typed expressions will return List<String>, Combobox List<WellStatus>.
								return ((List<?>) canonicalValue).stream().map(wellStatusToString).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						}), new FunctionDisplayConverter(canonicalValue -> {
							if (canonicalValue instanceof List) {
								// Manually typed expressions will return List<String>, Combobox List<EntityStatus.getCode()>.
								return ((List<?>) canonicalValue).stream().map(wellStatusToCodeString).collect(Collectors.joining(", "));
							}
							return canonicalValue;
						})
				);
			}
		}
	};

}
