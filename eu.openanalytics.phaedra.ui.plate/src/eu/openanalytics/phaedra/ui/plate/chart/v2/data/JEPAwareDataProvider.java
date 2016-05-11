package eu.openanalytics.phaedra.ui.plate.chart.v2.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.console.ConsoleManager;
import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.BaseDataProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.DataProviderSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.data.IJEPAwareDataProvider;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.JEPFormulaDialog2;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.ui.plate.Activator;

public abstract class JEPAwareDataProvider<ENTITY, ITEM> extends BaseDataProvider<ENTITY, ITEM> implements IJEPAwareDataProvider {

	private static final ExecutorService executor = Executors.newFixedThreadPool(PrefUtils.getNumberOfThreads());
	protected static final String OTHER_GROUP = "Other";
	protected static final String NO_GROUP = "No Group";

	private String[] jepExpressions;
	private List<float[]> calculatedValues;

	@Override
	public void initialize() {
		super.initialize();
		calculatedValues = new ArrayList<float[]>();
		if (jepExpressions == null) {
			jepExpressions = new String[] { "", "", "", "", "", "" };
		}
	}

	@Override
	public String getSelectedFeature(int axis) {
		String feature = super.getSelectedFeature(axis);
		if (feature.equalsIgnoreCase(EXPRESSIONSTRING)) {
			return jepExpressions[axis];
		}
		return feature;
	}

	@Override
	public int getFeatureIndex(String feature) {
		if (feature == null) return -1;
		if (feature.equalsIgnoreCase(EXPRESSIONSTRING) || CollectionUtils.contains(jepExpressions, feature)) {
			for (int i = 0; i < getFeatures().size(); i++) {
				if (getFeatures().get(i).equalsIgnoreCase(EXPRESSIONSTRING)) return i;
			}
		}
		return super.getFeatureIndex(feature);
	}

	/*@Override
	public void setSelectedFeature(String feature, int axis) {
		// Note: this method is called repeatedly (i.e. for each layer); do not prompt for a JEP expression here.
		// Instead, call generateJEPExpression(int dim) explicitly to prompt the user.
		super.setSelectedFeature(feature, axis);
	}*/

	@Override
	public void setAuxilaryFeature(String feature, int dimension, IProgressMonitor monitor) {
		if (feature.equalsIgnoreCase(EXPRESSIONSTRING)) generateJEPExpression(dimension);
		super.setAuxilaryFeature(feature, dimension, monitor);
	}

	@Override
	public String[] getJepExpressions() {
		return jepExpressions;
	}

	@Override
	public void setJepExpressions(String[] newExpressions) {
		this.jepExpressions = newExpressions;
		if (jepExpressions == null || jepExpressions.length < 6) {
			jepExpressions = new String[] { "", "", "", "", "", "" };
		}
		calculatedValues = new ArrayList<float[]>();
	}

	@Override
	public void loadData(List<ITEM> data, int dimensions, IProgressMonitor monitor) {
		calculatedValues.clear();
		super.loadData(data, dimensions, monitor);
	}

	public void generateJEPExpression(int dimension) {
		JEPFormulaDialog2 dialog = new JEPFormulaDialog2(null, PlateUtils.getProtocolClass(getPlate()));

		if (hasJepExpression(dimension)) {
			dialog.setFormula(getJepExpressions()[dimension]);
		}

		final AtomicInteger retCode = new AtomicInteger();
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				int r = dialog.open();
				retCode.set(r);
			}
		});
		
		if (retCode.get() == Dialog.OK) {
			getJepExpressions()[dimension] = dialog.getFormula();
			calculateJEPValues(true);
		}
	}

	public boolean hasJepExpression(int dimension) {
		if (getJepExpressions() != null) {
			// Since Parallel Coordinates Plot JEP expressions are also needed for 6+ dimensions.
			if (getJepExpressions().length <= dimension) {
				jepExpressions = Arrays.copyOfRange(getJepExpressions(), 0, dimension + 1);
				for (int i = 0; i < jepExpressions.length; i++) {
					if (jepExpressions[i] == null) {
						jepExpressions[i] = "";
					}
				}
			}

			if (getJepExpressions()[dimension] != null && !getJepExpressions()[dimension].isEmpty()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Convenience method for Parallel Coordinates Plot for unchecking a feature.
	 * Removes the given Feature name from the selected features list.
	 * Also updates the JEP Expression order.
	 *
	 * @param feature Name of the feature to remove.
	 */
	public void removeFeature(String feature) {
		int index = getSelectedFeatures().indexOf(feature);
		getSelectedFeatures().remove(feature);
		// In case a Feature is removed, update the JEP expressions to their new positions.
		if (index > -1) {
			for (int i = index; i < jepExpressions.length; i++) {
				if (i < jepExpressions.length-1) {
					jepExpressions[i] = jepExpressions[i+1];
				} else {
					jepExpressions[i] = "";
				}
			}
		}

		// Reset layer
		setDataBounds(null);
	}

	public abstract Plate getPlate();

	@Override
	public float[] getColumnData(int col, int axis) {
		if (col != -1 && col < getFeatures().size() - 1) {
			return super.getColumnData(col, axis);
		}

		int rowCount = getTotalRowCount();
		float[] columnData = new float[rowCount];

		if (!isDataAvailable(axis)) calculateJEPValues(false);

		if (isDataAvailable(axis)) {
			float[] data = calculatedValues.get(axis);
			for (int fullIndex = 0; fullIndex < rowCount; fullIndex++) {
				if (getCurrentFilter().get(fullIndex)) {
					columnData[fullIndex] = data[fullIndex];
				} else {
					// Items that do not match the filter should be set to NaN so they are not displayed.
					columnData[fullIndex] = Float.NaN;
				}
			}
		} else {
			// No data available, fill the data with NaN's.
			Arrays.fill(columnData, Float.NaN);
		}
		return columnData;
	}

	@Override
	public void loadFeature(String feature, IProgressMonitor monitor) {
		if (feature.equalsIgnoreCase(EXPRESSIONSTRING)) return;
		super.loadFeature(feature, monitor);
	}

	private boolean isDataAvailable(int axis) {
		return (calculatedValues.size() > axis && calculatedValues.get(axis).length == getTotalRowCount());
	}

	private void calculateJEPValues(boolean displayError) {

		calculatedValues = new ArrayList<float[]>();

		// warnings[0] == Contains NaN
		// warnings[1] == Different size
		final boolean[] warnings = new boolean[2];
		final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

		int totalRowCount = getTotalRowCount();
		for (int i = 0; i < getJepExpressions().length; i++) {
			final String expression = getJepExpressions()[i];
			calculatedValues.add(new float[0]);
			if (expression == null || expression.isEmpty()) continue;
			float[] allValues = new float[totalRowCount];
			Map<ENTITY, Future<float[]>> valueList = new HashMap<>();
			for (final ENTITY entity : getCurrentEntities()) {
				final int length = getDataSizes().get(entity);
				if (length == 0) continue;
				Callable<float[]> callable = createCallable(expression, entity, length, warnings, exceptions);
				valueList.put(entity, executor.submit(callable));
			}

			int iterator = 0;
			for (final ENTITY entity : getCurrentEntities()) {
				if (!valueList.containsKey(entity)) continue;
				float[] values;
				try {
					values = valueList.get(entity).get();
				} catch (InterruptedException | ExecutionException e) {
					values = new float[getDataSizes().get(entity)];
					Arrays.fill(values, Float.NaN);
					exceptions.add(e);
				}

				// If aggregation is used, the total row count is no
				// longer the sum of the entity row counts.
				if (iterator + values.length > allValues.length) continue;

				System.arraycopy(values, 0, allValues, iterator, values.length);
				iterator += values.length;
			}

			calculatedValues.set(i, allValues);
		}

		if (!exceptions.isEmpty() && displayError) {
			Display.getDefault().asyncExec(() -> {
				Exception e = exceptions.get(0);
				MessageDialog.openError(Display.getDefault().getActiveShell(), "JEP Expression Error", "Possible causes: incorrect expression\n\n" + e.getMessage());
				EclipseLog.error(e.getMessage(), e, Activator.getDefault());
			});
		}


		if (warnings[0]) {
			final String message = "Some expression results are Not-a-Number (NaN). These values will not be shown."
					+ "\nPossible causes: log 0, divide by 0, complex results, ...";
			if (displayError) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Not-a-Number values", message);
				});
			} else {
				ConsoleManager.getInstance().printErr(message);
			}
		}

		if (warnings[1]) {
			final String message = "The number of values returned by the expression differs from the expected number of values."
					+ "\nThis could be caused by a filter() function, but also by incorrect aggregation.";
			if (displayError) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Calculation returned a different length of values", message);
				});
			} else {
				ConsoleManager.getInstance().printErr(message);
			}
		}
	}

	private Callable<float[]> createCallable(final String expression, final ENTITY entity, final int length
			, final boolean[] warnings, final List<Exception> exceptions) {

		return () -> {
			float[] values = new float[length];
			try {
				float[] result = evaluateArray(expression, entity);
				if (result.length == length) {
					values = result;
					if (containsNaN(values)) warnings[0] = true;
				} else {
					warnings[0] = true;
					warnings[1] = true;
					// TODO: When JEP Expressions can return results that are longer, apply new logic here.
					System.arraycopy(result, 0, values, 0, Math.min(result.length, values.length));
					if (values.length > result.length) Arrays.fill(values, result.length, values.length, Float.NaN);
				}
			} catch (CalculationException e) {
				Arrays.fill(values, Float.NaN);
				exceptions.add(e);
			}

			return values;
		};
	}

	@Override
	public DataProviderSettings<ENTITY, ITEM> getDataProviderSettings() {
		DataProviderSettings<ENTITY, ITEM> settings = super.getDataProviderSettings();
		settings.setJepExpressions(getJepExpressions());
		return settings;
	}

	@Override
	public void setDataProviderSettings(DataProviderSettings<ENTITY, ITEM> settings) {
		super.setDataProviderSettings(settings);
		setJepExpressions(settings.getJepExpressions());
	}

	protected abstract float[] evaluateArray(String expression, ENTITY entity) throws CalculationException;

	private boolean containsNaN(float[] values) {
		for (int j = 0; j < values.length; j++) {
			if (Float.isNaN(values[j])) {
				return true;
			}
		}
		return false;
	}

}