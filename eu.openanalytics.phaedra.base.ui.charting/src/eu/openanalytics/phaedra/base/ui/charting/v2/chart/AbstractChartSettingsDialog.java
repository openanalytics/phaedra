package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;

public abstract class AbstractChartSettingsDialog<ENTITY, ITEM> extends TitleAreaDialog {

	private static Map<String, AbstractChartSettingsDialog<?, ?>> activeDialogMap = new HashMap<>();

	private AbstractChartLayer<ENTITY, ITEM> layer;
	private ChartSettings settings;

	public AbstractChartSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.MODELESS);
		setBlockOnOpen(true);
		this.layer = layer;
		this.settings = layer.getChartSettings();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Adjust chart settings");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(getTitle());
		setMessage(getTitleMessage());
		Composite area = (Composite) super.createDialogArea(parent);
		Composite comp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
		GridLayoutFactory.fillDefaults().numColumns(getNumberOfColumns()).margins(10, 10).applyTo(comp);
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				activeDialogMap.remove(AbstractChartSettingsDialog.this.getClass().getName());
			}
		});
		return embedDialogArea(comp);
	}

	@Override
	public int open() {
		String className = this.getClass().getName();
		if (activeDialogMap.containsKey(className)) {
			activeDialogMap.get(className).getShell().forceFocus();
			return CANCEL;
		}
		activeDialogMap.put(className, this);
		return super.open();
	}

	public int getNumberOfColumns() {
		return 2;
	}

	public String getTitle() {
		if (layer.getChart() != null) {
			return layer.getChart().getName().getDecription() + " Settings";
		}
		return "Chart Settings";
	}

	public String getTitleMessage() {
		return "Adjust the settings for this specific chart. \nThese settings will override the default settings in the charting preference page.";
	}

	public abstract Control embedDialogArea(Composite area);

	public AbstractChartLayer<ENTITY, ITEM> getLayer() {
		return layer;
	}

	public void setLayer(AbstractChartLayer<ENTITY, ITEM> layer) {
		this.layer = layer;
	}

	public ChartSettings getSettings() {
		return settings;
	}

	public void setSettings(ChartSettings settings) {
		this.settings = settings;
	}
}