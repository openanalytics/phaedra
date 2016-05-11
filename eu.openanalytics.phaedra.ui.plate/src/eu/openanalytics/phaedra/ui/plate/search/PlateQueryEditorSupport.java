package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.convert.DateDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.FunctionDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.Flag;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowseWells;
import eu.openanalytics.phaedra.validation.ValidationService.EntityStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateApprovalStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateUploadStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;

public class PlateQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Plate.class;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new PlateColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Plate plate = SelectionUtils.getFirstObject(sel, Plate.class);
				if (plate != null) BrowseWells.execute(plate);
			};
		});
	}

	private class PlateColumnAccessor extends RichColumnAccessor<Plate> {

		private String[] columns = {
				"Protocol", "Experiment", "ID", "Img", "SW", "Seq"
				, "Barcode", "C", "V", "A", "U", "Description", "Link Info"
				, "Calculation Date", "Calculation Error", "Validation Date", "Validation By"
				, "Approval Date", "Approval By", "Upload Date", "Upload By" };

		@Override
		public Object getDataValue(Plate rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getExperiment().getProtocol().getName();
			case 1:
				return rowObject.getExperiment().getName();
			case 2:
				return rowObject.getId();
			case 3:
				return rowObject.isImageAvailable();
			case 4:
				return rowObject.isSubWellDataAvailable();
			case 5:
				return rowObject.getSequence();
			case 6:
				return rowObject.getBarcode();
			case 7:
				return rowObject.getCalculationStatus();
			case 8:
				return rowObject.getValidationStatus();
			case 9:
				return rowObject.getApprovalStatus();
			case 10:
				return rowObject.getUploadStatus();
			case 11:
				return rowObject.getDescription();
			case 12:
				return rowObject.getInfo();
			case 13:
				return rowObject.getCalculationDate();
			case 14:
				return rowObject.getCalculationError();
			case 15:
				return rowObject.getCalculationDate();
			case 16:
				return rowObject.getValidationUser();
			case 17:
				return rowObject.getApprovalDate();
			case 18:
				return rowObject.getApprovalUser();
			case 19:
				return rowObject.getUploadDate();
			case 20:
				return rowObject.getUploadUser();
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
		public String getTooltipText(Plate rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 2) return "Plate ID";
				if (colIndex == 3) return "Image Available";
				if (colIndex == 4) return "Sub-well Data Available";
				if (colIndex == 5) return "Plate Sequence";
				if (colIndex == 7) return "Calculation Status";
				if (colIndex == 8) return "Validation Status";
				if (colIndex == 9) return "Approval Status";
				if (colIndex == 10) return "Upload Status";
			} else {
				if (colIndex == 7) return PlateCalcStatus.getByCode(rowObject.getCalculationStatus()).toString();
				if (colIndex == 8) return PlateValidationStatus.getByCode(rowObject.getValidationStatus()).toString();
				if (colIndex == 9) return PlateApprovalStatus.getByCode(rowObject.getApprovalStatus()).toString();
				if (colIndex == 10) return PlateUploadStatus.getByCode(rowObject.getUploadStatus()).toString();
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 150;
			columnWidths[1] = 150;
			columnWidths[2] = 75;
			columnWidths[3] = 35;
			columnWidths[4] = 35;
			columnWidths[5] = 50;
			columnWidths[6] = 150;
			columnWidths[7] = 35;
			columnWidths[8] = 35;
			columnWidths[9] = 35;
			columnWidths[10] = 35;
			columnWidths[13] = 110;
			columnWidths[15] = 110;
			columnWidths[17] = 110;
			columnWidths[19] = 110;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			painters.put(new int[] { 3 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "image_link.png")));
			painters.put(new int[] { 4 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "package_link.png")));

			painters.put(new int[] { 7 }, new FlagCellPainter(
					new FlagMapping(FlagFilter.Negative, Flag.Red)
					, new FlagMapping(FlagFilter.Zero, Flag.White)
					, new FlagMapping(FlagFilter.Positive, Flag.Green)
					));
			painters.put(new int[] { 8, 9, 10 }, new FlagCellPainter(
					new FlagMapping(FlagFilter.Negative, Flag.Red)
					, new FlagMapping(FlagFilter.One, Flag.Blue)
					, new FlagMapping(FlagFilter.GreaterThanOne, Flag.Green)
					, new FlagMapping(FlagFilter.All, Flag.White)
					));

			return painters;
		}

		@Override
		public IConfiguration getCustomConfiguration() {
			return new PlateTableConfiguration();
		}

		private class PlateTableConfiguration extends AbstractRegistryConfiguration {
			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				int[] dateColumns = new int[] { 13, 15, 17, 19 };
				for (int dateColumn : dateColumns) {
					configRegistry.registerConfigAttribute(
							CellConfigAttributes.DISPLAY_CONVERTER
							, new DateDisplayConverter("dd/MM/yyyy HH:mm:ss")
							, DisplayMode.NORMAL
							, columns[dateColumn]
					);
				}
				applyStatusFilter(configRegistry, 7, PlateCalcStatus.class);
				applyStatusFilter(configRegistry, 8, PlateValidationStatus.class);
				applyStatusFilter(configRegistry, 9, PlateApprovalStatus.class);
				applyStatusFilter(configRegistry, 10, PlateUploadStatus.class);
			}

			private <E extends Enum<E>> void applyStatusFilter(IConfigRegistry configRegistry, int column, Class<E> enumData) {
				NatTableUtils.applyAdvancedComboFilter(configRegistry, column, Arrays.asList(enumData.getEnumConstants())
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

	}

}
