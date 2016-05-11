package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.line.KernelDensity1DWekaChartSettingsDialog;

public class SubWellDensityWeka1DConfigDialog extends SubWellScatter2DConfigDialog {

	private Text numberOfBins;
	private Button cumulativeBtn;
	
	private int numberOfBinsValue;
	private boolean cumulativeValue;
	
	public SubWellDensityWeka1DConfigDialog(Shell parentShell, SubWellDensityWeka1DLayer layer) {
		super(parentShell, layer);
		
		this.numberOfBinsValue = getSettings().getNumberOfBins();
		this.cumulativeValue = getSettings().isCumulative();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control area = super.createDialogArea(parent);
		
		Composite settingsComp = findSettingsPanel();
		if (settingsComp == null) {
			return area;
		}
		
		Label lblBinWidth = new Label(settingsComp, SWT.NONE);
		lblBinWidth.setText("Number of bins: ");

		numberOfBins = new Text(settingsComp, SWT.SINGLE | SWT.BORDER);
		numberOfBins.setText(String.valueOf(this.numberOfBinsValue));
		numberOfBins.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				Text t = (Text) e.widget;
				getSettings().setNumberOfBins(Math.min(KernelDensity1DWekaChartSettingsDialog.MAX_NUMBER_OF_BINS, Integer.parseInt(t.getText())));
				t.setText(String.valueOf(getSettings().getNumberOfBins()));
			}

			@Override
			public void focusGained(FocusEvent e) {
				Text t = (Text) e.widget;
				t.selectAll();
			}
		});
		GridDataFactory.fillDefaults().applyTo(numberOfBins);

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
		
		return area;
	}
	
	@Override
	protected void createFilterTab(TabFolder tabFolder) {
		// Do nothing.
		//TODO Some filters are useful, but the welltype filter messes up the dialog height.
	}
	
	@Override
	protected boolean showFillOption() {
		return false;
	}
	
	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		getSettings().setNumberOfBins(numberOfBinsValue);
		getSettings().setCumulative(cumulativeValue);
		super.cancelPressed();
	}
	
	private Composite findSettingsPanel() {
		TabItem[] tabs = getTabFolder().getItems();
		for (TabItem tab: tabs) {
			if (tab.getText().equals("Settings")) {
				return (Composite)tab.getControl();
			}
		}
		return null;
	}
}