package eu.openanalytics.phaedra.ui.curve.search;

import java.util.ArrayList;
import java.util.Arrays;
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
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
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
			List<String> colNames = new ArrayList<>();
			colNames.add("Protocol");
			colNames.add("Experiment");
			colNames.add("Plate");
			colNames.add("Feature");
			colNames.add("Compounds");
			colNames.add("Model");
			colNames.add("Fit Version");
			colNames.add("Fit Date");
			colNames.add("Fit Error");
			
			Arrays.stream(CurveFitService.getInstance().getFitModels())
					.flatMap(s -> Arrays.stream(CurveFitService.getInstance().getModel(s).getOutputParameters()))
					.filter(d -> d.key)
					.distinct()
					.sorted((d1, d2) -> d1.name.compareTo(d2.name))
					.forEach(def -> colNames.add(def.name));
			columns = colNames.toArray(new String[colNames.size()]);
		}

		@Override
		public Object getDataValue(CRCurve curve, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return ((Protocol) curve.getAdapter(Protocol.class)).getName();
			case 1:
				return ((Experiment) curve.getAdapter(Experiment.class)).getName();
			case 2:
				return ((Plate) curve.getAdapter(Plate.class)).getBarcode();
			case 3:
				return curve.getFeature();
			case 4:
				return curve.getCompounds();
			case 5:
				return curve.getModel();
			case 6:
				return curve.getFitVersion();
			case 7:
				return curve.getFitDate();
			case 8:
				return curve.getErrorCode();
			default:
				Curve c = CurveFitService.getInstance().getCurve(curve.getId());
				Value v = CurveParameter.find(c.getOutputParameters(), columns[columnIndex]);
				return CurveParameter.renderValue(v, c, null);
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
