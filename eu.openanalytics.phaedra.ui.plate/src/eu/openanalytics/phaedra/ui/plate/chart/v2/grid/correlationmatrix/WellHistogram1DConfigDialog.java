package eu.openanalytics.phaedra.ui.plate.chart.v2.grid.correlationmatrix;

import java.awt.Color;
import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;

public class WellHistogram1DConfigDialog extends WellChartConfigDialog {

	private Text numberOfBins;
	private Combo barType;
	private ColorSelector colorSelector;
	private Button normalizedBtn;
	private Button cumulativeBtn;
	private Button logaritmicBtn;

	private static String[] BAR_TYPES = new String[] { DefaultStyleProvider.BARSTYLE_FILLED,
			DefaultStyleProvider.BARSTYLE_FILLED_3D, DefaultStyleProvider.BARSTYLE_OPEN,
			DefaultStyleProvider.BARSTYLE_SPIKES, DefaultStyleProvider.BARSTYLE_STEPS };

	private int numberOfBinsValue;
	private String barTypeValue;
	private Color colorValue;
	private boolean logaritmicValue;
	private boolean normalizedValue;
	private boolean cumulativeValue;

	public WellHistogram1DConfigDialog(Shell parentShell, WellHistogram1DLayer layer) {
		super(parentShell, layer);
		// previous values
		this.numberOfBinsValue = getSettings().getNumberOfBins();
		this.barTypeValue = getSettings().getDefaultSymbolType();
		this.colorValue = getSettings().getDefaultColor();
		this.logaritmicValue = getSettings().isLogaritmic();
		this.cumulativeValue = getSettings().isCumulative();
		this.normalizedValue = getSettings().isNormalized();
		
		if (barTypeValue.isEmpty()) {
			barTypeValue = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_BAR_TYPE);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		createSettingsTab(getTabFolder());

		return area;
	}

	private void createSettingsTab(TabFolder tabFolder) {
		TabItem settingsTab = new TabItem(tabFolder, SWT.NONE);
		settingsTab.setText("Settings");
		
		final Composite settingsComp = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(settingsComp);

		Label lblBinWidth = new Label(settingsComp, SWT.NONE);
		lblBinWidth.setText("Number of bins: ");

		numberOfBins = new Text(settingsComp, SWT.SINGLE | SWT.BORDER);
		numberOfBins.setText(String.valueOf(this.numberOfBinsValue));
		numberOfBins.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				Text t = (Text) e.widget;
				getSettings().setNumberOfBins(Integer.parseInt(t.getText()));
			}

			@Override
			public void focusGained(FocusEvent e) {
				Text t = (Text) e.widget;
				t.selectAll();
			}
		});
		GridDataFactory.fillDefaults().applyTo(numberOfBins);

		Label lblColor = new Label(settingsComp, SWT.NONE);
		lblColor.setText("Default color for chart points:");

		colorSelector = new ColorSelector(settingsComp);
		colorSelector.setColorValue(new RGB(colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue()));
		colorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (!event.getNewValue().equals(event.getOldValue())) {
					RGB newValue = (RGB) event.getNewValue();
					getSettings().setDefaultColor(new Color(newValue.red, newValue.green, newValue.blue));
				}
			}
		});

		Label lblType = new Label(settingsComp, SWT.NONE);
		lblType.setText("Default symbol style:");

		barType = new Combo(settingsComp, SWT.READ_ONLY);
		barType.setItems(BAR_TYPES);
		barType.select(Arrays.asList(BAR_TYPES).indexOf(barTypeValue));
		barType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getSettings().setDefaultSymbolType(BAR_TYPES[barType.getSelectionIndex()]);
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(barType);

		normalizedBtn = new Button(settingsComp, SWT.CHECK);
		normalizedBtn.setSelection(getSettings().isNormalized());
		normalizedBtn.setText("Normalized");
		normalizedBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				getSettings().setNormalized(btn.getSelection());
				if (btn.getSelection()) {
					logaritmicBtn.setSelection(false);
				}
				logaritmicBtn.setEnabled(!btn.getSelection());
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(normalizedBtn);

		logaritmicBtn = new Button(settingsComp, SWT.CHECK);
		logaritmicBtn.setSelection(getSettings().isLogaritmic());
		logaritmicBtn.setText("Logaritmic");
		logaritmicBtn.setEnabled(!getSettings().isNormalized());
		logaritmicBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				getSettings().setLogaritmic(btn.getSelection());
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(logaritmicBtn);

		cumulativeBtn = new Button(settingsComp, SWT.CHECK);
		cumulativeBtn.setSelection(getSettings().isCumulative());
		cumulativeBtn.setText("Cumulative");
		cumulativeBtn.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button btn = (Button) e.getSource();
				getSettings().setCumulative(btn.getSelection());
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(cumulativeBtn);
		
		settingsTab.setControl(settingsComp);
	}

	@Override
	protected void cancelPressed() {
		getSettings().setNumberOfBins(numberOfBinsValue);
		getSettings().setDefaultSymbolType(barTypeValue);
		getSettings().setDefaultColor(colorValue);
		getSettings().setLogaritmic(logaritmicValue);
		getSettings().setCumulative(cumulativeValue);
		getSettings().setNormalized(normalizedValue);
		super.cancelPressed();
	}
}