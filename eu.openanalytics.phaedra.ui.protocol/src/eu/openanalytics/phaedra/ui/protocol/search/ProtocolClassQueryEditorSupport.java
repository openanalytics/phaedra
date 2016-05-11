package eu.openanalytics.phaedra.ui.protocol.search;

import java.util.Arrays;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.cmd.BrowseProtocols;

public class ProtocolClassQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return ProtocolClass.class;
	}

	@Override
	public String getLabel() {
		return "Protocol Class";
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new ProtocolClassColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				ProtocolClass pc = SelectionUtils.getFirstObject(sel, ProtocolClass.class);
				if (pc != null) BrowseProtocols.execute(pc);
			};
		});
	}

	private class ProtocolClassColumnAccessor extends RichColumnAccessor<ProtocolClass> {

		private String[] columns = { "ID", "Name", "Description", "Well Features", "Subwell Features", "Image Channels" };

		@Override
		public Object getDataValue(ProtocolClass rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getId();
			case 1:
				return rowObject.getName();
			case 2:
				return rowObject.getDescription();
			case 3:
				return rowObject.getFeatures().size();
			case 4:
				return rowObject.getSubWellFeatures().size();
			case 5:
				return rowObject.getImageSettings().getImageChannels().size();
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
		public String getTooltipText(ProtocolClass rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 0) return "Protocol Class ID";
				if (colIndex == 1) return "Protocol Class Name";
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
			return columnWidths;
		}
	};

}
