package eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class Axes2DChartSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private Text textChartTitle;
	private String chartTitle;

	private Text[] textAxisLabels;
	private String[] axisLabels;

	private Button showGridlines;
	private boolean showGridLinesValue;

	public Axes2DChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);
		this.axisLabels = new String[0];
		this.chartTitle = "";
		if (getLayer().getDataProvider() != null) {
			if (layer.getDataProvider().getTitle() != null) {
				this.chartTitle = layer.getDataProvider().getTitle();
			}
			this.axisLabels = getLayer().getDataProvider().getAxisLabels();
		}
		this.showGridLinesValue = getSettings().isShowGridLines();
	}

	@Override
	public Control embedDialogArea(Composite area) {
		Label label = new Label(area, SWT.NONE);
		label.setText("Chart Title: ");

		textChartTitle = new Text(area, SWT.BORDER);
		textChartTitle.setText(chartTitle);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(textChartTitle);

		textAxisLabels = new Text[axisLabels.length];
		for (int i = 0; i < axisLabels.length; i++) {
			label = new Label(area, SWT.NONE);
			label.setText("Axis " + (i+1) + ": ");
			textAxisLabels[i] = new Text(area, SWT.BORDER);
			textAxisLabels[i].setText(axisLabels[i]);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(textAxisLabels[i]);
		}

		showGridlines = new Button(area, SWT.CHECK);
		showGridlines.setSelection(showGridLinesValue);
		showGridlines.setText("Show gridlines");
		showGridlines.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getSettings().setShowGridLines(showGridlines.getSelection());
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(showGridlines);
		return area;
	}

	@Override
	protected void okPressed() {
		if (getLayer().getDataProvider() != null) {
			boolean hasNewValue = false;

			String newTitle = textChartTitle.getText();
			if (newTitle.isEmpty()) {
				newTitle = null;
			}
			if (newTitle == null || !newTitle.equals(getLayer().getDataProvider().getTitle())) {
				getLayer().getDataProvider().setTitle(newTitle);
				getLayer().getDataProvider().getTitleChangedObservable().valueChanged(newTitle);
				hasNewValue = true;
			}

			String[] labels = getLayer().getDataProvider().getCustomAxisLabels();
			if (labels == null) {
				labels = new String[textAxisLabels.length];
			}
			for (int i = 0; i < textAxisLabels.length; i++) {
				String lbl = textAxisLabels[i].getText();
				if (!lbl.equals(axisLabels[i]) && !lbl.equals(labels[i])) {
					labels[i] = lbl;
					hasNewValue = true;
				}
			}

			if (hasNewValue) {
				getLayer().getDataProvider().setCustomAxisLabels(labels);
				getLayer().getDataProvider().getLabelChangedObservable().valueChanged(labels);
				getLayer().dataChanged();
			}
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		getSettings().setShowGridLines(showGridLinesValue);
		getLayer().settingsChanged();
		super.cancelPressed();
	}

}