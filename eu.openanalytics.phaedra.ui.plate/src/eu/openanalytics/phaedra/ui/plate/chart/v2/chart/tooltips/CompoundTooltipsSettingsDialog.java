package eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips;

import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipProvider.CFG_SHOW_STRUCTURE;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipProvider.CFG_SIZE;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipProvider.DEFAULT_SHOW_STRUCTURE;
import static eu.openanalytics.phaedra.ui.plate.chart.v2.chart.tooltips.CompoundTooltipProvider.DEFAULT_SIZE;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.tooltips.TooltipSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class CompoundTooltipsSettingsDialog<ENTITY, ITEM> extends TooltipSettingsDialog<ENTITY, ITEM> {

	private Spinner sizeSpinner;
	private Button showStructureButton;

	private int size;
	private boolean showStructure;

	public CompoundTooltipsSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);
		Object o = getSettings().getTooltipSettings().getMiscSetting(CFG_SIZE);
		if (o instanceof Integer) {
			this.size = (int) o;
		} else {
			this.size = DEFAULT_SIZE;
		}
		o = getSettings().getTooltipSettings().getMiscSetting(CFG_SHOW_STRUCTURE);
		if (o instanceof Boolean) {
			this.showStructure = (boolean) o;
		} else {
			this.showStructure = DEFAULT_SHOW_STRUCTURE;
		}
	}

	@Override
	public Control embedDialogArea(Composite area) {
		Label lbl = new Label(area, SWT.NONE);
		lbl.setText("Tooltip Size:");

		sizeSpinner = new Spinner(area, SWT.BORDER);
		sizeSpinner.setEnabled(showStructure);
		sizeSpinner.setMinimum(50);
		sizeSpinner.setMaximum(300);
		sizeSpinner.setSelection(size);
		sizeSpinner.addListener(SWT.Selection, e -> {
			getSettings().getTooltipSettings().setMiscSetting(CFG_SIZE, sizeSpinner.getSelection());
			getLayer().settingsChanged();
		});

		lbl = new Label (area, SWT.NONE);
		lbl.setText("Show Structure:");

		showStructureButton = new Button(area, SWT.CHECK);
		showStructureButton.setSelection(showStructure);
		showStructureButton.addListener(SWT.Selection, e -> {
			sizeSpinner.setEnabled(showStructureButton.getSelection());
			getSettings().getTooltipSettings().setMiscSetting(CFG_SHOW_STRUCTURE, showStructureButton.getSelection());
			getLayer().settingsChanged();
		});

		return super.embedDialogArea(area);
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
		getSettings().getTooltipSettings().setMiscSetting(CFG_SIZE, size);
		getSettings().getTooltipSettings().setMiscSetting(CFG_SHOW_STRUCTURE, showStructure);
		getLayer().settingsChanged();
	}

}