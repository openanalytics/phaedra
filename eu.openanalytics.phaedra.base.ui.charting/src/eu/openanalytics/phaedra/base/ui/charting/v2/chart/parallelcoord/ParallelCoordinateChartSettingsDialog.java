package eu.openanalytics.phaedra.base.ui.charting.v2.chart.parallelcoord;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter.Scatter2DChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public class ParallelCoordinateChartSettingsDialog<ENTITY, ITEM> extends Scatter2DChartSettingsDialog<ENTITY, ITEM> {

	private Button clearSelection;

	private boolean isClearSelection;

	public ParallelCoordinateChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		this.isClearSelection = getSettings().isShowFog();
	}

	@Override
	public Control embedDialogArea(Composite area) {
		super.embedDialogArea(area);

		showLines.setVisible(false);

		clearSelection = new Button(area, SWT.CHECK);
		clearSelection.setSelection(isClearSelection);
		clearSelection.setText("Highlight Selection");
		clearSelection.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getSettings().setShowFog(clearSelection.getSelection());
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(clearSelection);

		return area;
	}

	@Override
	protected void cancelPressed() {
		getSettings().setShowFog(isClearSelection);
		super.cancelPressed();
	}

}