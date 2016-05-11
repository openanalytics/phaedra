package eu.openanalytics.phaedra.ui.subwell.chart.v2.grid.correlationmatrix;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.ChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.LayerSettings;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.FeatureEntityLayer;

public abstract class SubWellChartConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	public static final int AUXILARY_DIMENSION_COUNT = 3;

	private TabFolder tabFolder;
	private SubWellChartLayer layer;

	private String fillOption;
	private String prevFillOption;

	public SubWellChartConfigDialog(Shell parentShell, SubWellChartLayer layer) {
		super(parentShell);
		this.layer = layer;
		this.fillOption = layer.getFillOption();
		this.prevFillOption = fillOption;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configuration: " + layer.getName());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		setTitle(layer.getName());
		setMessage("Configure the properties of the chart below.");

		layer.getRenderer().resetRendering();

		Composite container = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);

		tabFolder = new TabFolder(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		return area;
	}

	@Override
	protected void okPressed() {
		// persist settings in the grid state
		GridState.saveValue(layer.getProtocolId(), layer.getId(), SubWellChartLayer.PROPERTY_CONFIG, getConfig());
		GridState.saveValue(layer.getProtocolId(), layer.getId(), SubWellChartLayer.FILL_OPTION, fillOption);

		layer.setFillOption(fillOption);
		layer.getRenderer().resetRendering();

		if (layer.isRequiresDataLoad()) {
			layer.triggerLoadDataJob();
		} else {
			layer.getLayerSupport().getViewer().getGrid().redraw();
		}

		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		layer.setFillOption(prevFillOption);
		super.cancelPressed();
	}

	protected void createFillOptionCombo(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Fill Option:");

		final Combo fillOptionCmd = new Combo(parent, SWT.READ_ONLY);
		fillOptionCmd.setItems(FeatureEntityLayer.FILL_OPTIONS);
		int select = 0;
		for (int i = 0; i < FeatureEntityLayer.FILL_OPTIONS.length; i++) {
			if (fillOption.equalsIgnoreCase(FeatureEntityLayer.FILL_OPTIONS[i])) {
				select = i;
				break;
			}
		}
		fillOptionCmd.select(select);
		fillOptionCmd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fillOption = fillOptionCmd.getText();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fillOptionCmd);
	}

	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		this.layer.getRenderer().resetRendering();
		viewer.getGrid().redraw();
	}

	/* getter and setters */
	public TabFolder getTabFolder() {
		return tabFolder;
	}

	public SubWellChartLayer getLayer() {
		return layer;
	}

	public LayerSettings<Well, Well> getConfig() {
		return getLayer().getLayerSettings();
	}

	public ChartSettings getSettings() {
		return getConfig().getChartSettings();
	}

}