package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
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
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.ui.plate.cmd.ShowCompoundInspector;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;

public class CompoundQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Compound.class;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new CompoundColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Compound comp = SelectionUtils.getFirstObject(sel, Compound.class);
				if (comp != null) ShowCompoundInspector.execute();
			};
		});
	}
	
	private class CompoundColumnAccessor extends RichColumnAccessor<Compound> {

		private String[] columns = { "Protocol", "Experiment", "Plate", "V", "Compound Type", "Compound Nr", "Saltform" };

		@Override
		public Object getDataValue(Compound rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getPlate().getExperiment().getProtocol().getName();
			case 1:
				return rowObject.getPlate().getExperiment().getName();
			case 2:
				return rowObject.getPlate().getBarcode();
			case 3:
				return rowObject.getValidationStatus();
			case 4:
				return rowObject.getType();
			case 5:
				return rowObject.getNumber();
			case 6:
				return rowObject.getSaltform();
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
		public String getTooltipText(Compound rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 3) return "Validation Status";
				if (colIndex == 6) return "Saltform";
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 200;
			columnWidths[1] = 200;
			columnWidths[3] = 35;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			int[] flagColumns = new int[] { 3 };
			painters.put(flagColumns, new FlagCellPainter("curve",
					new FlagMapping(FlagFilter.Negative, Flag.Red),
					new FlagMapping(FlagFilter.Zero, Flag.White),
					new FlagMapping(FlagFilter.One, Flag.Blue),
					new FlagMapping(FlagFilter.GreaterThanOne, Flag.Green)
			));

			return painters;
		}

		@Override
		public IConfiguration getCustomConfiguration() {
			return new CompoundTableConfiguration();
		}

		private class CompoundTableConfiguration extends AbstractRegistryConfiguration {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				NatTableUtils.applyAdvancedComboFilter(configRegistry, 3, Arrays.asList(CompoundValidationStatus.values())
					, new DefaultDisplayConverter()
					, new FunctionDisplayConverter(canonicalValue -> {
						if (canonicalValue instanceof List) {
							return ((List<?>) canonicalValue).stream().map(Object::toString).collect(Collectors.joining(", "));
						}
						return canonicalValue;
					}), new FunctionDisplayConverter(canonicalValue -> {
						if (canonicalValue instanceof List) {
							// Manually typed expressions will return List<String>, Combobox List<EntityStatus.getCode()>.
							return ((List<?>) canonicalValue).stream().map(o -> {
								if (o instanceof EntityStatus) return ((EntityStatus) o).getCode()+"";
								else return o.toString();
							}).collect(Collectors.joining(", "));
						}
						return canonicalValue;
					})
				);
			}
		}
	};

}
