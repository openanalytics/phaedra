package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.layer.ScriptedChartLayer.ScriptedChartConfig;

public class ScriptedChartConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	private ScriptedChartLayer layer;
	private ScriptedChartConfig config;
	
	private Combo scriptSrcCmb;
	
	public ScriptedChartConfigDialog(Shell parentShell, ScriptedChartLayer layer, ScriptedChartConfig config) {
		super(parentShell);
		this.layer = layer;
		this.config = config;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configuration: Scripted Chart");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);
		
		new Label(container, SWT.NONE).setText("Script:");
		
		scriptSrcCmb = new Combo(container, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(scriptSrcCmb);
		try {
			List<String> scripts = ScriptService.getInstance().getCatalog().getAvailableScripts("");
			scriptSrcCmb.setItems(scripts.toArray(new String[scripts.size()]));
			if (config != null && config.getScriptSrc() != null) scriptSrcCmb.select(scripts.indexOf(config.getScriptSrc()));
		} catch (IOException e) {
			EclipseLog.warn("Failed to list scripts from catalog", e, Activator.PLUGIN_ID);
		}
	
		setTitle("Scripted Chart");
		setMessage("Configure the chart settings below");
		return area;
	}
	
	@Override
	protected void okPressed() {
		config.setScriptSrc(scriptSrcCmb.getText());
		layer.update();
		super.okPressed();
	}
	
	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		ScriptedChartLayer.class.cast(layer).update();
	}

}
