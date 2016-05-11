package eu.openanalytics.phaedra.base.ui.charting.v2.chart.scatter;

import java.util.Arrays;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.TopcatViewStyles;

public class Scatter3DChartSettingsDialog<ENTITY, ITEM> extends Scatter2DChartSettingsDialog<ENTITY, ITEM> {

	private boolean showFogValue;
	private String viewStyleValue;
	private String[] viewStyles;
	private Button showFog;
	private Combo viewStyle;

	public Scatter3DChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);
		this.showFogValue = getSettings().isShowFog();
		this.viewStyleValue = getSettings().getViewStyle();
		this.viewStyles = TopcatViewStyles.STYLES;
	}

	@Override
	public Control embedDialogArea(Composite area) {
		super.embedDialogArea(area);

		showFog = new Button(area, SWT.CHECK);
		showFog.setSelection(showFogValue);
		showFog.setText("Show fog");
		showFog.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				getSettings().setShowFog(showFog.getSelection());
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(showFog);

		Label lblType = new Label(area, SWT.NONE);
		lblType.setText("Orientation:");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(lblType);

		viewStyle = new Combo(area, SWT.READ_ONLY);
		viewStyle.setItems(viewStyles);
		viewStyle.select(Arrays.asList(viewStyles).indexOf(viewStyleValue));
		viewStyle.addSelectionListener(new SelectionAdapter() {
			@Override
			@SuppressWarnings("rawtypes")
			public void widgetSelected(SelectionEvent e) {
				String view = viewStyles[viewStyle.getSelectionIndex()];
				getSettings().setViewStyle(view);
				((Scatter3DChart)getLayer().getChart()).setViewStyle(view);
				getLayer().settingsChanged();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(viewStyle);

		// TODO: Implement Connect Lines to Plot3D
		showLines.dispose();

		return area;
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		getSettings().setShowFog(showFogValue);
		getSettings().setViewStyle(viewStyleValue);
		super.cancelPressed();
	}
}
