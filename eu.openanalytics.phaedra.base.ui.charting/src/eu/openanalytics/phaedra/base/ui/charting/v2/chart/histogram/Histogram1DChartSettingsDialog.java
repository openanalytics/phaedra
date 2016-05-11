package eu.openanalytics.phaedra.base.ui.charting.v2.chart.histogram;

import java.awt.Color;
import java.util.Arrays;
import java.util.BitSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.util.misc.ValueObservable;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class Histogram1DChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private Spinner opacitySpinner;
	private Text numberOfBins;
	private Combo barType;
	private ColorSelector colorSelector;
	private Button normalizedBtn;
	private Button cumulativeBtn;
	private Button logaritmicBtn;

	private static final int FILTER_TIMEOUT = 1000;
	private Job binJob;

	public final static int MIN_NUMBER_OF_BINS = 7;
	public final static int MAX_NUMBER_OF_BINS = 1000;

	private static String[] BAR_TYPES = new String[] { DefaultStyleProvider.BARSTYLE_FILLED,
		DefaultStyleProvider.BARSTYLE_FILLED_3D, DefaultStyleProvider.BARSTYLE_OPEN,
		DefaultStyleProvider.BARSTYLE_SPIKES, DefaultStyleProvider.BARSTYLE_STEPS };

	private int selectionOpacityValue;
	private int numberOfBinsValue;
	private String barTypeValue;
	private Color colorValue;
	private boolean logaritmicValue;
	private boolean normalizedValue;
	private boolean cumulativeValue;
	private ValueObservable observable;

	public Histogram1DChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer,
			ValueObservable observable) {

		super(parentShell, layer);
		// previous values
		this.selectionOpacityValue = getSettings().getSelectionOpacity();
		this.numberOfBinsValue = getSettings().getNumberOfBins();
		this.barTypeValue = getSettings().getDefaultSymbolType();
		this.colorValue = getSettings().getDefaultColor();
		this.logaritmicValue = getSettings().isLogaritmic();
		this.cumulativeValue = getSettings().isCumulative();
		this.normalizedValue = getSettings().isNormalized();
		this.observable = observable;

		if (barTypeValue.isEmpty()) {
			barTypeValue = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_BAR_TYPE);
		}
	}

	@Override
	public Control embedDialogArea(Composite area) {
		Label lblOpac = new Label(area, SWT.NONE);
		lblOpac.setText("Transparancy of non-selected points: ");
		GridDataFactory.fillDefaults().applyTo(lblOpac);

		opacitySpinner = new Spinner(area, SWT.BORDER);
		opacitySpinner.setMinimum(1);
		opacitySpinner.setMaximum(100);
		opacitySpinner.setIncrement(1);
		opacitySpinner.setPageIncrement(10);
		opacitySpinner.setDigits(2);
		opacitySpinner.setSelection(this.selectionOpacityValue);
		opacitySpinner.addListener(SWT.Selection, e -> {
			int opacity = opacitySpinner.getSelection();
			getSettings().setSelectionOpacity(opacity);
			getLayer().settingsChanged();
		});

		GridDataFactory.fillDefaults().applyTo(opacitySpinner);

		Label lblBinWidth = new Label(area, SWT.NONE);
		lblBinWidth.setText("Number of bins: ");
		GridDataFactory.fillDefaults().applyTo(lblBinWidth);
		numberOfBins = new Text(area, SWT.SINGLE | SWT.BORDER);
		numberOfBins.setText(String.valueOf(this.numberOfBinsValue));
		numberOfBins.addListener(SWT.Modify, e -> {
			binJob.cancel();
			binJob.schedule(FILTER_TIMEOUT);
		});
		GridDataFactory.fillDefaults().applyTo(numberOfBins);

		Label lblColor = new Label(area, SWT.NONE);
		lblColor.setText("Default color for chart points:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblColor);

		colorSelector = new ColorSelector(area);
		colorSelector.setColorValue(new RGB(colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue()));
		colorSelector.addListener(event -> {
			if (!event.getNewValue().equals(event.getOldValue())) {
				RGB newValue = (RGB) event.getNewValue();
				getSettings().setDefaultColor(new Color(newValue.red, newValue.green, newValue.blue));
				getLayer().settingsChanged();
			}
		});

		Label lblType = new Label(area, SWT.NONE);
		lblType.setText("Default symbol style:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		barType = new Combo(area, SWT.READ_ONLY);
		barType.setItems(BAR_TYPES);
		barType.select(Arrays.asList(BAR_TYPES).indexOf(barTypeValue));
		barType.addListener(SWT.Selection, e -> {
			getSettings().setDefaultSymbolType(BAR_TYPES[barType.getSelectionIndex()]);
			getLayer().settingsChanged();
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(barType);

		normalizedBtn = new Button(area, SWT.CHECK);
		normalizedBtn.setSelection(getSettings().isNormalized());
		normalizedBtn.setText("Normalized");
		normalizedBtn.addListener(SWT.Selection, e -> {
			boolean selection = normalizedBtn.getSelection();
			getSettings().setNormalized(selection);
			if (selection) {
				logaritmicBtn.setSelection(false);
				getSettings().setLogaritmic(false);
			}
			logaritmicBtn.setEnabled(!selection);

			getLayer().dataChanged();
			observable.valueChanged();
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(normalizedBtn);

		logaritmicBtn = new Button(area, SWT.CHECK);
		logaritmicBtn.setSelection(getSettings().isLogaritmic());
		logaritmicBtn.setText("Logaritmic");
		logaritmicBtn.setEnabled(!getSettings().isNormalized());
		logaritmicBtn.addListener(SWT.Selection, e -> {
			getSettings().setLogaritmic(logaritmicBtn.getSelection());
			getLayer().dataChanged();
			observable.valueChanged();
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(logaritmicBtn);

		cumulativeBtn = new Button(area, SWT.CHECK);
		cumulativeBtn.setSelection(getSettings().isCumulative());
		cumulativeBtn.setText("Cumulative");
		cumulativeBtn.addListener(SWT.Selection, e -> {
			getSettings().setCumulative(cumulativeBtn.getSelection());
			getLayer().dataChanged();
			observable.valueChanged();
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(cumulativeBtn);

		this.binJob = new Job("Setting number of bins") {
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
		getSettings().setSelectionOpacity(selectionOpacityValue);
		getSettings().setNumberOfBins(numberOfBinsValue);
		getSettings().setDefaultSymbolType(barTypeValue);
		getSettings().setDefaultColor(colorValue);
		getSettings().setLogaritmic(logaritmicValue);
		getSettings().setCumulative(cumulativeValue);
		getSettings().setNormalized(normalizedValue);
		getLayer().settingsChanged();
		super.cancelPressed();
	}

	private void applyNumberOfBins() {
		if (numberOfBins.isDisposed()) return;
		final String value = numberOfBins.getText();
		if (NumberUtils.isDigit(value)) {
			int bins = Integer.parseInt(value);
			bins = Math.max(bins, MIN_NUMBER_OF_BINS);
			bins = Math.min(bins, MAX_NUMBER_OF_BINS);
			getSettings().setNumberOfBins(bins);
			getLayer().dataChanged();
			// Disable selection since it is no longer valid.
			getLayer().getChart().setSelection(new BitSet());
			observable.valueChanged();
		}
	}

}
