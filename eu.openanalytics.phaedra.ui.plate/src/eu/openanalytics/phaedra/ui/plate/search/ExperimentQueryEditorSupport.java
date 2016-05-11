package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.convert.DateDisplayConverter;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.ui.plate.cmd.BrowsePlates;

public class ExperimentQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Experiment.class;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new ExperimentColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Experiment exp = SelectionUtils.getFirstObject(sel, Experiment.class);
				if (exp != null) BrowsePlates.execute(exp);
			};
		});
	}
	
	private class ExperimentColumnAccessor extends RichColumnAccessor<Experiment> {

		private String[] columns = { "ID", "Name", "Created On", "Creator", "Description", "Comment", "Protocol" };

		@Override
		public Object getDataValue(Experiment rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getId();
			case 1:
				return rowObject.getName();
			case 2:
				return rowObject.getCreateDate();
			case 3:
				return rowObject.getCreator();
			case 4:
				return rowObject.getDescription();
			case 5:
				return (rowObject.getComments() != null && !rowObject.getComments().isEmpty());
			case 6:
				return rowObject.getProtocol().getName();
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
		public String getTooltipText(Experiment rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 0) return "Experiment ID";
				if (colIndex == 1) return "Experiment Name";
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 50;
			columnWidths[1] = 250;
			columnWidths[2] = 110;
			columnWidths[4] = 200;
			columnWidths[5] = 75;
			columnWidths[6] = 200;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			painters.put(new int[] { 5 }, new FlagCellPainter(""
					, new FlagMapping(FlagFilter.NegativeOrZero, "page_white.png")
					, new FlagMapping(FlagFilter.Positive, "page_edit.png"))
			);

			return painters;
		}

		@Override
		public IConfiguration getCustomConfiguration() {
			return new AbstractRegistryConfiguration() {
				@Override
				public void configureRegistry(IConfigRegistry configRegistry) {
					configRegistry.registerConfigAttribute(
							CellConfigAttributes.DISPLAY_CONVERTER
							, new DateDisplayConverter("dd/MM/yyyy HH:mm:ss")
							, DisplayMode.NORMAL
							, columns[2]
					);
				}
			};
		}
	};

}
