package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config;

import java.util.HashMap;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class HeatmapConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	private BaseGridLayer layer;
	private IColorMethod colorMethod;

	private Combo typeCombo;
	private int selectedType;

	public HeatmapConfigDialog(Shell parentShell, BaseGridLayer layer, IColorMethod colorMethod) {
		super(parentShell);

		this.layer = layer;
		this.colorMethod = colorMethod;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configuration: " + layer.getName());
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Label:");

		typeCombo = new Combo(container, SWT.READ_ONLY);
		typeCombo.setItems(HeatmapConfig.VALUE_TYPES);
		typeCombo.select(((HeatmapConfig) layer.getConfig()).getValueType());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(typeCombo);

		new Label(container, SWT.NONE);
		Button modifyColor = new Button(container, SWT.PUSH);
		modifyColor.setToolTipText("Change the color method");
		modifyColor.setImage(IconManager.getIconImage("palette.png"));
		modifyColor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				colorMethod.createDialog(getParentShell()).open();
			}
		});

		setTitle(layer.getName());
		setMessage("Configure the color and correlation method for the grid.");
		return area;
	}

	@Override
	protected void okPressed() {
		selectedType = typeCombo.getSelectionIndex();
		applySettings(layer.getLayerSupport().getViewer(), layer);
		super.okPressed();
	}

	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		HeatmapConfig cfg = (HeatmapConfig) layer.getConfig();
		cfg.setValueType(selectedType, layer.getId());

		HashMap<String, String> settings = new HashMap<>();
		colorMethod.getConfiguration(settings);
		colorMethod.configure(settings);
		cfg.setColorMethod(settings);

		viewer.getGrid().redraw();
	}

}