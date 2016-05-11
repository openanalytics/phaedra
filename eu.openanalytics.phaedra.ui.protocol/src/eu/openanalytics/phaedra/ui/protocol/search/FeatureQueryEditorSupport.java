package eu.openanalytics.phaedra.ui.protocol.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.painter.cell.AbstractCellPainter;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagFilter;
import eu.openanalytics.phaedra.base.ui.nattable.painter.FlagCellPainter.FlagMapping;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.protocol.util.FeaturePropertyProvider;

public class FeatureQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return Feature.class;
	}

	@Override
	public String getLabel() {
		return "Well Feature";
	}

	@Override
	public String getLabelForField(String fieldName) {
		switch (fieldName) {
			case "shortName": return "alias";
		}
		return super.getLabelForField(fieldName);
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new FeatureColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				Feature feature = SelectionUtils.getFirstObject(sel, Feature.class);
				if (feature != null) {
					try {
						String viewId = "eu.openanalytics.phaedra.ui.plate.inspector.feature.FeatureInspector";
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
					} catch (PartInitException ex) {}
				}
			};
		});
	}

	private class FeatureColumnAccessor extends RichColumnAccessor<Feature> {

		private String[] columns = { "ID", "Name", "Protocol Class", "Key", "Req", "Num", "Calc", "Fit"
				, "Curve", "LC", "HC", "Alias", "Description", "Group" };

		@Override
		public Object getDataValue(Feature rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return rowObject.getId();
			case 1:
				return rowObject.getName();
			case 2:
				return rowObject.getProtocolClass().getName();
			case 3:
				return rowObject.isKey();
			case 4:
				return rowObject.isRequired();
			case 5:
				return rowObject.isNumeric();
			case 6:
				return rowObject.isCalculated();
			case 7:
				return rowObject.getCurveSettings().containsKey(CurveSettings.KIND);
			case 8:
				return FeaturePropertyProvider.getValue("Curve", rowObject);
			case 9:
				return ProtocolUtils.getLowType(rowObject);
			case 10:
				return ProtocolUtils.getHighType(rowObject);
			case 11:
				return rowObject.getShortName();
			case 12:
				return rowObject.getDescription();
			case 13:
				return rowObject.getFeatureGroup() != null ? rowObject.getFeatureGroup().getName() : "";
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
		public String getTooltipText(Feature rowObject, int colIndex) {
			if (rowObject == null) {
				if (colIndex == 0) return "Feature ID";
				if (colIndex == 1) return "Feature Name";
				if (colIndex == 3) return "Key Feature";
				if (colIndex == 4) return "Required Feature";
				if (colIndex == 5) return "Numeric Feature";
				if (colIndex == 6) return "Calculated Feature";
				if (colIndex == 7) return "Fitted Feature";
				if (colIndex == 9) return "Low Control";
				if (colIndex == 10) return "High Control";
				if (colIndex == 13) return "Feature Group";
			}

			return super.getTooltipText(rowObject, colIndex);
		}

		@Override
		public int[] getColumnWidths() {
			int[] columnWidths = new int[getColumnCount()];
			Arrays.fill(columnWidths, -1);
			columnWidths[0] = 50;
			columnWidths[1] = 250;
			columnWidths[2] = 150;
			columnWidths[3] = 35;
			columnWidths[4] = 35;
			columnWidths[5] = 35;
			columnWidths[6] = 35;
			columnWidths[7] = 35;
			columnWidths[8] = 150;
			columnWidths[9] = 75;
			columnWidths[10] = 75;
			columnWidths[12] = 150;
			return columnWidths;
		}

		@Override
		public Map<int[], AbstractCellPainter> getCustomCellPainters() {
			Map<int[], AbstractCellPainter> painters = new HashMap<>();

			painters.put(new int[] { 3 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "key.png")));
			painters.put(new int[] { 4 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "star.png")));
			painters.put(new int[] { 5 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "calc.png")));
			painters.put(new int[] { 6 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "aggregation.gif")));
			painters.put(new int[] { 7 }, new FlagCellPainter("", new FlagMapping(FlagFilter.One, "curve.png")));

			return painters;
		}

	};

}
