package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import java.awt.Color;
import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.Activator;
import eu.openanalytics.phaedra.base.ui.charting.preferences.Prefs;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.grouping.DefaultStyleProvider;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class Scatter2DChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	public static final String CONNECT_POINTS = "Connect points";
	public final static String[] SYMBOL_TYPES = new String[] {
		DefaultStyleProvider.FILLED_CIRCLE, DefaultStyleProvider.OPEN_CIRCLE,
		DefaultStyleProvider.OPEN_RECTANGLE, DefaultStyleProvider.FILLED_RECTANGLE };
	public final static String[] SYMBOL_SIZES = new String[] { "0", "1", "2", "3", "4", "5" };

	private Spinner opacitySpinner;
	private ColorSelector colorSelector;
	private Combo symbolType;
	private Combo symbolSize;
	protected Button showLines;

	// Current settings
	private int selectionOpacityValue;
	private Color colorValue;
	private String symbolTypeValue;
	private String symbolSizeValue;
	private boolean showLinesValue;

	public Scatter2DChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		this.selectionOpacityValue = getSettings().getSelectionOpacity();
		this.colorValue = getSettings().getDefaultColor();
		this.symbolTypeValue = getSettings().getDefaultSymbolType();
		this.symbolSizeValue = String.valueOf(getSettings().getDefaultSymbolSize());
		this.showLinesValue = getSettings().isLines();

		if (symbolTypeValue.isEmpty()) {
			symbolTypeValue = Activator.getDefault().getPreferenceStore().getString(Prefs.DEFAULT_SYMBOL_TYPE);
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
		opacitySpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int opacity = opacitySpinner.getSelection();
				getSettings().setSelectionOpacity(opacity);
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().applyTo(opacitySpinner);

		Label lblSize = new Label(area, SWT.NONE);
		lblSize.setText("Default symbol size:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblSize);

		symbolSize = new Combo(area, SWT.READ_ONLY);
		symbolSize.setItems(SYMBOL_SIZES);
		int index = Arrays.asList(SYMBOL_SIZES).indexOf(symbolSizeValue);
		symbolSize.select(index);
		symbolSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().setDefaultSymbolSize(Integer.parseInt(SYMBOL_SIZES[symbolSize.getSelectionIndex()]));
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolSize);

		Label lblColor = new Label(area, SWT.NONE);
		lblColor.setText("Default color for chart points:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblColor);

		colorSelector = new ColorSelector(area);
		colorSelector.setColorValue(new RGB(colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue()));
		colorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (!event.getNewValue().equals(event.getOldValue())) {
					RGB newValue = (RGB) event.getNewValue();
					getSettings().setDefaultColor(new Color(newValue.red, newValue.green, newValue.blue));
					getLayer().settingsChanged();
				}
			}
		});

		Label lblType = new Label(area, SWT.NONE);
		lblType.setText("Default symbol style:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		symbolType = new Combo(area, SWT.READ_ONLY);
		symbolType.setItems(SYMBOL_TYPES);
		int selected = 0;
		for (String symbol : SYMBOL_TYPES) {
			if (symbol.equalsIgnoreCase(symbolTypeValue)) {
				break;
			}
			selected++;
		}
		symbolType.select(selected);
		symbolType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().setDefaultSymbolType(SYMBOL_TYPES[symbolType.getSelectionIndex()]);
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(symbolType);

		showLines = new Button(area, SWT.CHECK);
		showLines.setSelection(showLinesValue);
		showLines.setText(CONNECT_POINTS);
		// If Aux axes present, do not allow turning on showLines.
		showLines.setEnabled(showLinesValue || getLayer().getDataProvider().getAuxiliaryFeatures().size() == 0);
		showLines.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getSettings().setLines(showLines.getSelection());
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(showLines);
		return area;
	}

	@Override
	protected void cancelPressed() {
		// Restore all values to previous settings
		getSettings().setDefaultColor(colorValue);
		getSettings().setDefaultSymbolType(symbolTypeValue);
		getSettings().setSelectionOpacity(selectionOpacityValue);
		getSettings().setLines(showLinesValue);
		getSettings().setDefaultSymbolSize(Integer.parseInt(symbolSizeValue));
		getLayer().settingsChanged();
		super.cancelPressed();
	}
}