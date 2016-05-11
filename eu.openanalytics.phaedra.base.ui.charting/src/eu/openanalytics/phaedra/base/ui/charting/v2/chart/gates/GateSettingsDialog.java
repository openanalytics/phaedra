package eu.openanalytics.phaedra.base.ui.charting.v2.chart.gates;

import java.awt.Color;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class GateSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private ColorSelector colorSelector;
	private Spinner sizeSpinner;
	private Spinner opacitySpinner;
	private Color colorValue;
	private float opacityValue;
	private int sizeValue;

	public GateSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);
		this.colorValue = getSettings().getGateSettings().gateColor;
		this.opacityValue = getSettings().getGateSettings().gateOpacity;
		this.sizeValue = getSettings().getGateSettings().gateSize;
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Gate Settings");
	}

	@Override
	public String getTitle() {
		return "Gate Settings";
	}

	@Override
	public String getTitleMessage() {
		return "A plate can have a gate attached to it, if the gate was saved during the FACS analysis."
				+ "\nYou can specify the display settings of the gate below.";
	}

	@Override
	public Control embedDialogArea(Composite area) {

		Label lblColor = new Label(area, SWT.NONE);
		lblColor.setText("Gate color:");
		GridDataFactory.fillDefaults().applyTo(lblColor);
		colorSelector = new ColorSelector(area);
		colorSelector.setColorValue(new RGB(colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue()));
		colorSelector.addListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB color = (RGB) event.getNewValue();
				getSettings().getGateSettings().gateColor = new Color(color.red, color.green, color.blue);
				getLayer().settingsChanged();
			}
		});

		Label lblSize = new Label(area, SWT.NONE);
		lblSize.setText("Border size:");
		GridDataFactory.fillDefaults().applyTo(lblSize);
		sizeSpinner = new Spinner(area, SWT.BORDER);
		sizeSpinner.setMinimum(1);
		sizeSpinner.setMaximum(10);
		sizeSpinner.setIncrement(1);
		sizeSpinner.setSelection(sizeValue);
		sizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().getGateSettings().gateSize = sizeSpinner.getSelection();
				getLayer().settingsChanged();
			};
		});
		GridDataFactory.fillDefaults().applyTo(sizeSpinner);

		Label lblOpacity = new Label(area, SWT.NONE);
		lblOpacity.setText("Transparancy:");
		GridDataFactory.fillDefaults().applyTo(lblOpacity);
		opacitySpinner = new Spinner(area, SWT.BORDER);
		opacitySpinner.setMinimum(0);
		opacitySpinner.setMaximum(100);
		opacitySpinner.setIncrement(1);
		opacitySpinner.setPageIncrement(10);
		opacitySpinner.setDigits(2);
		opacitySpinner.setSelection((int) ((1 - opacityValue) * 100));
		opacitySpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().getGateSettings().gateOpacity = 1 - ((float) opacitySpinner.getSelection() / 100);
				getLayer().settingsChanged();
			};
		});
		return area;
	}

	@Override
	protected void cancelPressed() {
		getSettings().getGateSettings().gateColor = colorValue;
		getSettings().getGateSettings().gateOpacity = opacityValue;
		getSettings().getGateSettings().gateSize = sizeValue;

		super.cancelPressed();
	}
}