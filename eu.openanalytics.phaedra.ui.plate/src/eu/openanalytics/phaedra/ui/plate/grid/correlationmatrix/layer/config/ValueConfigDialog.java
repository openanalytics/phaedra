package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix.layer.config;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;

public class ValueConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	private BaseGridLayer layer;

	private Combo[] typeCombos;
	private Combo fontColorCombo;

	private int[] selectedTypes;
	private int selectedFontColor;

	public ValueConfigDialog(Shell parentShell, BaseGridLayer layer) {
		super(parentShell);

		this.layer = layer;
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

		ValueConfig config = (ValueConfig) layer.getConfig();

		typeCombos = new Combo[config.getValueTypeLength()];
		for (int i = 0; i < typeCombos.length; i++) {
			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Label " + (i + 1) + ":");

			typeCombos[i] = new Combo(container, SWT.READ_ONLY);
			typeCombos[i].setItems(ValueConfig.VALUE_TYPES);
			typeCombos[i].select(config.getValueType(i));
			GridDataFactory.fillDefaults().grab(true, false).applyTo(typeCombos[i]);
		}

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Font color:");

		fontColorCombo = new Combo(container, SWT.READ_ONLY);
		String[] labels = new String[]{ "Based on heatmap color", "Black", "White" };
		fontColorCombo.setItems(labels);
		fontColorCombo.select(config.getFontColor()-1);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fontColorCombo);

		setTitle(layer.getName());
		setMessage("Select the labels to display on the grid.");
		return area;
	}

	@Override
	protected void okPressed() {
		selectedTypes = new int[typeCombos.length];
		for (int i = 0; i < typeCombos.length; i++) {
			selectedTypes[i] = typeCombos[i].getSelectionIndex();
		}
		selectedFontColor = fontColorCombo.getSelectionIndex() + 1;
		applySettings(layer.getLayerSupport().getViewer(), layer);
		super.okPressed();
	}

	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		ValueConfig cfg = (ValueConfig)layer.getConfig();
		for (int i = 0; i < typeCombos.length; i++) {
			cfg.setValueType(i, selectedTypes[i], layer.getId());
		}
		cfg.setFontColor(selectedFontColor, layer.getId());
		viewer.getGrid().redraw();
	}

}