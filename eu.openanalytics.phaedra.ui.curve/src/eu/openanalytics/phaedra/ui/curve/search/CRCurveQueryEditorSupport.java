package eu.openanalytics.phaedra.ui.curve.search;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.search.AbstractQueryEditorSupport;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.curve.cmd.ShowCrcDetails;

public class CRCurveQueryEditorSupport extends AbstractQueryEditorSupport {

	@Override
	public Class<?> getSupportedClass() {
		return CRCurve.class;
	}

	@Override
	public String getLabel() {
		return "Curve";
	}

	@Override
	public IRichColumnAccessor<?> getColumnAccessor() {
		return new CRCurveColumnAccessor();
	}

	@Override
	public void customize(NatTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				CRCurve curve = SelectionUtils.getFirstObject(sel, CRCurve.class);
				if (curve != null) ShowCrcDetails.execute();
			};
		});
	}

	private class CRCurveColumnAccessor extends RichColumnAccessor<CRCurve> {

		private String[] columns;

		public CRCurveColumnAccessor() {
			List<String> tempColumns = new ArrayList<>();
			tempColumns.add("Protocol");
			tempColumns.add("Experiment");
			tempColumns.add("Plate");

			List<String> methodNames = new ArrayList<>();
			Method[] methods = CRCurve.class.getDeclaredMethods();
			for (Method m: methods) {
				String methodName = m.getName();
				if (methodName.startsWith("get") && !methodName.equals("getParent")) {
					methodNames.add(methodName);
				}
			}
			Collections.sort(methodNames);

			methodNames.forEach(methodName -> tempColumns.add(methodName.substring(3)));

			columns = tempColumns.toArray(new String[methodNames.size()]);
		}

		@Override
		public Object getDataValue(CRCurve rowObject, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return ((Protocol) rowObject.getAdapter(Protocol.class)).getName();
			case 1:
				return ((Experiment) rowObject.getAdapter(Experiment.class)).getName();
			case 2:
				return ((Plate) rowObject.getAdapter(Plate.class)).getBarcode();
			default:
				return ReflectionUtils.invoke("get" + columns[columnIndex], rowObject);
			}
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
	};

}
