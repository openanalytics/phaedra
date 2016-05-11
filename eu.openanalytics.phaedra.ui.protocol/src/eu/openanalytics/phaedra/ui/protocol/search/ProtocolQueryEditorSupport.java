package eu.openanalytics.phaedra.ui.protocol.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ProtocolQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Protocol.class;
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new ProtocolColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Protocol protocol = SelectionUtils.getFirstObject(sel, Protocol.class);
				if (protocol != null) {
					try {
						IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
						handlerService.executeCommand("eu.openanalytics.phaedra.ui.plate.cmd.BrowseExperiments", null);
					} catch (Exception ex) {}
				}
			};
		});
	}

	private class ProtocolColumnAccessor extends RichColumnAccessor<Protocol> {

		private String[] columns = { "ID", "Name", "Description", "Protocol Class", "Team Code"
				, "Protocol Owner(s)", "Upload System" };

		@Override
		public Object getDataValue(Protocol rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getId();
			case 1:
				return rowObject.getName();
			case 2:
				return rowObject.getDescription();
			case 3:
				return rowObject.getProtocolClass().getName();
			case 4:
				return rowObject.getTeamCode();
			case 5:
				return StringUtils.createSeparatedString(rowObject.getOwners(), ",");
			case 6:
				return rowObject.getUploadSystem();
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
		public String getTooltipText(Protocol rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 0) return "Protocol ID";
				if (colIndex == 1) return "Protocol Name";
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 35;
			columnWidths[1] = 250;
			columnWidths[2] = 200;
			columnWidths[3] = 150;
			columnWidths[4] = 150;
			columnWidths[5] = 100;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			int[] flagColumns = new int[] { 6 };
			painters.put(flagColumns, new TextPainter(false, true) {
				@Override
				public void paintCell(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
					paintFg = false;
					super.paintCell(cell, gc, rectangle, configRegistry);

					Object valueObject = cell.getDataValue();
					if (valueObject instanceof String) {
						Image icon = IconManager.getIconImage("upload/" + valueObject.toString().toLowerCase() + ".png");

						if (icon != null) {
							int centerX = rectangle.x + rectangle.width/2;
							int centerY = rectangle.y + rectangle.height/2;
							Rectangle bounds = icon.getBounds();
							gc.drawImage(icon, centerX - bounds.width/2, centerY - bounds.height/2);
						}
					}
				}
			});

			return painters;
		}
	};

}
