package eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class TooltipSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private int fontSize;
	private boolean showLabelsValue;
	private boolean showCoordsValue;

	private Spinner fontSizeSpinner;

	public TooltipSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);
		this.fontSize = getSettings().getTooltipSettings().getFontSize();
		this.showLabelsValue = getSettings().getTooltipSettings().isShowLabels();
		this.showCoordsValue = getSettings().getTooltipSettings().isShowCoords();
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Tooltip Settings");
	}

	@Override
	public String getTitle() {
		return "Tooltip Settings";
	}

	@Override
	public String getTitleMessage() {
		return "You can specify the display settings of the tooltips below.";
	}

	@Override
	public Control embedDialogArea(Composite area) {

		Label lblLabel = new Label(area, SWT.NONE);
		lblLabel.setText("Show labels:");

		Button checkboxButton = new Button(area, SWT.CHECK);
		checkboxButton.setSelection(showLabelsValue);
		checkboxButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().getTooltipSettings().setShowLabels(!getSettings().getTooltipSettings().isShowLabels());
				getLayer().settingsChanged();
			}
		});

		lblLabel = new Label(area, SWT.NONE);
		lblLabel.setText("Show coordinates:");

		checkboxButton = new Button(area, SWT.CHECK);
		checkboxButton.setSelection(showCoordsValue);
		checkboxButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().getTooltipSettings().setShowCoords(!getSettings().getTooltipSettings().isShowCoords());
				getLayer().settingsChanged();
			}
		});

		lblLabel = new Label(area, SWT.NONE);
		lblLabel.setText("Font size:");

		fontSizeSpinner = new Spinner(area, SWT.BORDER);
		// Everything under size 9 becomes blurry and unreadable.
		fontSizeSpinner.setMinimum(9);
		// If you still can't read it at size 15 you should consider buying a magnifier.
		fontSizeSpinner.setMaximum(15);
		fontSizeSpinner.setSelection(fontSize);
		fontSizeSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSettings().getTooltipSettings().setFontSize(fontSizeSpinner.getSelection());
				getLayer().settingsChanged();
			}
		});

		return area;
	}

	@Override
	protected void cancelPressed() {
		getSettings().getTooltipSettings().setFontSize(fontSize);
		getSettings().getTooltipSettings().setShowLabels(showLabelsValue);
		getSettings().getTooltipSettings().setShowCoords(showCoordsValue);

		super.cancelPressed();
	}
}
