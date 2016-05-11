package eu.openanalytics.phaedra.base.ui.charting.v2.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.charting.v2.data.IDataProvider;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class MinMaxFilter<ENTITY, ITEM> extends AbstractFilter<ENTITY, ITEM> {

	private static final String MIN = "MIN";
	private static final String MAX = "MAX";

	private Double[] min;
	private Double[] max;

	private final static List<String> DIMENSION_NAMES = new ArrayList<String>() {
		private static final long serialVersionUID = 1991497787247356189L;
		{
			add("X");
			add("Y");
			add("Z");
		}
	};

	public MinMaxFilter(int dimensions, IDataProvider<ENTITY, ITEM> dataProvider) {
		super("Min-Max", dataProvider);
		min = new Double[dimensions];
		max = new Double[dimensions];
		// When more then 3 dimensions use numbers instead of XYZ.
		if (dimensions > 3) {
			List<String> dimNames = new ArrayList<>();
			for (int i = 1; i <= dimensions; i++) {
				dimNames.add(i + "");
			}
			setFilterItems(dimNames);
		} else {
			setFilterItems(DIMENSION_NAMES.subList(0, dimensions));
		}
	}

	@Override
	public void filter() {
		for (String filter : getActiveFilterItems()) {
			int dimension = getDimension(filter);
			String feature = getDataProvider().getSelectedFeatures().get(dimension);
			int featureIndex = getDataProvider().getFeatureIndex(feature);

			float[] data = getDataProvider().getColumnData(featureIndex, dimension);
			for (int i = 0; i < getDataProvider().getTotalRowCount(); i++) {
				double value = data[i];
				if ((min[dimension] != null && value < min[dimension])
						|| (max[dimension] != null && value > max[dimension])) {
					getDataProvider().getCurrentFilter().set(i, false);
				}
			}
		}
	}

	@Override
	public boolean isActive() {
		return getActiveFilterItems() != null && !getActiveFilterItems().isEmpty();
	}

	private int getDimension(String filter) {
		for (int i = 0; i < getFilterItems().size(); i++) {
			if (getFilterItems().get(i).equals(filter)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void doApplyFilterItem(String filterItem) {
		int dimension = getDimension(filterItem);
		MinMaxDialog dialog = new MinMaxDialog(Display.getDefault().getActiveShell(), filterItem, min[dimension],
				max[dimension]);
		if (Window.OK == dialog.open()) {
			min[dimension] = dialog.getMin();
			max[dimension] = dialog.getMax();
		}

		// override default behavior(toggling)
		getActiveFilterItems().remove(filterItem);
		if (min[dimension] != null || max[dimension] != null) {
			getActiveFilterItems().add(filterItem);
		}

		super.doApplyFilterItem(filterItem);
	}

	public Double[] getMin() {
		return min;
	}

	public Double[] getMax() {
		return max;
	}

	public void setMin(Double[] min) {
		this.min = min;
	}

	public void setMax(Double[] max) {
		this.max = max;
	}

	@Override
	public Object getProperties() {
		Map<String, Double[]> minMaxValues = new HashMap<>();
		minMaxValues.put(MIN, getMin());
		minMaxValues.put(MAX, getMax());
		return minMaxValues;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setProperties(Object o) {
		if (o instanceof HashMap<?, ?>) {
			setMin(((HashMap<String, Double[]>) o).get(MIN));
			setMax(((HashMap<String, Double[]>) o).get(MAX));

			for (String name : getFilterItems()) {
				int dimension = getDimension(name);

				getActiveFilterItems().remove(name);
				if ((min.length > dimension && min[dimension] != null) || (max.length > dimension && max[dimension] != null)) {
					getActiveFilterItems().add(name);
				}

				super.doApplyFilterItem(name);
			}
		}
	}

	private static class MinMaxDialog extends TitleAreaDialog {

		private String dim;
		private Double min;
		private Double max;
		private Text minTxt;
		private Text maxTxt;

		protected MinMaxDialog(Shell parentShell, String dim, Double min, Double max) {
			super(parentShell);
			this.dim = dim;
			this.min = min;
			this.max = max;
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText(dim + " Range Filter");
			shell.setSize(300, 210);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			setTitle(dim + " Range Filter");
			setMessage("Specify a range or leave a field blank to set no min or max.");

			Composite container = new Composite(area, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(container);

			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Min:");

			minTxt = new Text(container, SWT.BORDER);
			if (min != null) {
				minTxt.setText(String.valueOf(min));
			}
			GridDataFactory.fillDefaults().grab(true, false).applyTo(minTxt);

			lbl = new Label(container, SWT.NONE);
			lbl.setText("Max:");

			maxTxt = new Text(container, SWT.BORDER);
			if (max != null) {
				maxTxt.setText(String.valueOf(max));
			}
			GridDataFactory.fillDefaults().grab(true, false).applyTo(maxTxt);

			return area;
		}

		@Override
		protected void okPressed() {
			min = null;
			max = null;

			String minString = minTxt.getText();
			String maxString = maxTxt.getText();
			boolean isEmptyMin = minString.isEmpty();
			boolean isEmptyMax = maxString.isEmpty();
			boolean isValidMin = isEmptyMin || NumberUtils.isDouble(minString);
			boolean isValidMax = isEmptyMax || NumberUtils.isDouble(maxString);
			if (isValidMin && isValidMax) {
				if (!isEmptyMin) min = Double.parseDouble(minString);
				if (!isEmptyMax) max = Double.parseDouble(maxString);
			} else {
				String errorMessage = "Please specify a valid numeric value (e.g. 75.376).\n";
				if (!isValidMin) errorMessage += "\n" + minString + " is not a valid min value.";
				if (!isValidMax) errorMessage += "\n" + maxString + " is not a valid max value.";
				MessageDialog.openError(getShell(), "Error", errorMessage);
				return;
			}

			super.okPressed();
		}

		public Double getMin() {
			return min;
		}

		public Double getMax() {
			return max;
		}
	}

}