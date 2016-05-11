package eu.openanalytics.phaedra.base.ui.charting.v2.chart.line;

import static eu.openanalytics.phaedra.base.ui.charting.v2.data.KernelDensityWekaDataCalculator.DEFAULT_PRECISION_VALUE;
import static eu.openanalytics.phaedra.base.ui.charting.v2.data.KernelDensityWekaDataCalculator.PRECISION_VALUE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;

public class KernelDensity1DWekaChartSettingsDialog<ENTITY, ITEM> extends Scatter2DChartSettingsDialog<ENTITY, ITEM> {

	private Spinner numberOfBins;
	private Spinner precisionSpinner;
	private Button cumulativeBtn;
	private int numberOfBinsValue;
	private int precisionValue;
	private boolean cumulativeValue;
	private ValueObservable observable;

	private static final int FILTER_TIMEOUT = 1000;
	private Job binJob;

	public final static int MIN_NUMBER_OF_BINS = 7;
	public final static int MAX_NUMBER_OF_BINS = 1000;

	public KernelDensity1DWekaChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer, ValueObservable observable) {
		super(parentShell, layer);
		this.numberOfBinsValue = getSettings().getNumberOfBins();
		this.cumulativeValue = getSettings().isCumulative();

		getSettings().getMiscSettings().putIfAbsent(PRECISION_VALUE, DEFAULT_PRECISION_VALUE);
		this.precisionValue = getSettings().getIntMiscSetting(PRECISION_VALUE);

		this.observable = observable;
	}

	@Override
	public Control embedDialogArea(Composite area) {
		super.embedDialogArea(area);

		cumulativeBtn = new Button(area, SWT.CHECK);
		cumulativeBtn.setSelection(getSettings().isCumulative());
		cumulativeBtn.setText("Cumulative");
		cumulativeBtn.addListener(SWT.Selection, e -> {
			getSettings().setCumulative(cumulativeBtn.getSelection());
			getLayer().dataChanged();
			observable.valueChanged();
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(cumulativeBtn);

		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Number of bins:");
		numberOfBins = new Spinner(area, SWT.BORDER);
		numberOfBins.setMinimum(MIN_NUMBER_OF_BINS);
		numberOfBins.setMaximum(MAX_NUMBER_OF_BINS);
		numberOfBins.setIncrement(1);
		numberOfBins.setPageIncrement(10);
		numberOfBins.setSelection(numberOfBinsValue);
		numberOfBins.addListener(SWT.Selection, e -> {
			binJob.cancel();
			binJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().applyTo(numberOfBins);

		lbl = new Label(area, SWT.NONE);
		lbl.setText("Precision:");
		precisionSpinner = new Spinner(area, SWT.BORDER);
		precisionSpinner.setMinimum(1);
		precisionSpinner.setMaximum(100000);
		precisionSpinner.setIncrement(1);
		precisionSpinner.setPageIncrement(10);
		precisionSpinner.setSelection(precisionValue);
		precisionSpinner.addListener(SWT.Selection, e -> {
			getSettings().getMiscSettings().put(PRECISION_VALUE, precisionSpinner.getSelection() + "");
			getLayer().dataChanged();
			observable.valueChanged();
		});
		GridDataFactory.fillDefaults().applyTo(precisionSpinner);

		this.binJob = new Job("Filtering") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(() -> applyNumberOfBins());
				return Status.OK_STATUS;
			}
		};

		return area;
	}

	@Override
	protected void okPressed() {
		// Make sure the job runs if scheduled.
		if (binJob.getState() == Job.SLEEPING) {
			binJob.cancel();
			applyNumberOfBins();
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		// Restore all values to previous settings
		getSettings().setNumberOfBins(numberOfBinsValue);
		getSettings().setCumulative(cumulativeValue);
		getSettings().getMiscSettings().put(PRECISION_VALUE, precisionValue + "");
		getLayer().dataChanged();
		observable.valueChanged();
		super.cancelPressed();
	}

	private void applyNumberOfBins() {
		if (numberOfBins.isDisposed()) return;
		int bins = numberOfBins.getSelection();
		getSettings().setNumberOfBins(bins);
		getLayer().dataChanged();
		observable.valueChanged();
	}

}