package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class PieChartConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

	private Plate plate;
	private PieChartLayer layer;
	private PieChartConfig config;

	private List<Feature> allFeatures;

	private Combo[] featureCombos;
	private ColorSelector[] featureColors;
	private Combo sizeFeatureCombo;

	public PieChartConfigDialog(Shell parentShell, Plate plate, PieChartLayer layer, PieChartConfig config) {
		super(parentShell);
		this.plate = plate;
		this.layer = layer;
		this.config = config;
		this.allFeatures = PlateUtils.getFeatures(plate);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configuration: Pie Chart");
	}

	@Override
	protected Point getInitialSize() {
		return super.getInitialSize();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);

		List<String> featureNames = CollectionUtils.transform(allFeatures, ProtocolUtils.FEATURE_NAMES);
		String[] nameArray = new String[featureNames.size() + 1];
		nameArray[0] = "<None>";
		for (int i = 0; i < featureNames.size(); i++) nameArray[i + 1] = featureNames.get(i);

		featureCombos = new Combo[PieChartConfig.MAX_PIE_FEATURES];
		featureColors = new ColorSelector[PieChartConfig.MAX_PIE_FEATURES];

		for (int i = 0; i < featureCombos.length; i++) {
			Label lbl = new Label(container, SWT.NONE);
			lbl.setText("Feature " + (i + 1) + ":");

			featureCombos[i] = new Combo(container, SWT.READ_ONLY);
			featureCombos[i].setItems(nameArray);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(featureCombos[i]);

			featureCombos[i].select(0);
			for (int j=0; j<allFeatures.size(); j++) {
				if (allFeatures.get(j).getId() == config.pieFeatures[i]) featureCombos[i].select(j+1);
			}

			featureColors[i] = new ColorSelector(container);
			featureColors[i].setColorValue(config.getColor(i));
			GridDataFactory.fillDefaults().grab(true, false).applyTo(featureCombos[i]);
		}

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Pie Size:");

		sizeFeatureCombo = new Combo(container, SWT.READ_ONLY);
		sizeFeatureCombo.setItems(nameArray);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sizeFeatureCombo);
		sizeFeatureCombo.select(0);
		for (int j=0; j<allFeatures.size(); j++) {
			if (allFeatures.get(j).getId() == config.sizeFeature) sizeFeatureCombo.select(j+1);
		}

		setTitle("Pie Chart");
		setMessage("Select up to " + PieChartConfig.MAX_PIE_FEATURES + " features to display in the pie."
				+ "\nOptionally, select one additional feature to represent the size of the pie.");
		return area;
	}

	@Override
	protected void okPressed() {
		// Save combo values into session.
		for (int i = 0; i < featureCombos.length; i++) {
			int index = featureCombos[i].getSelectionIndex();
			if (index > 0) {
				Feature selectedFeature = allFeatures.get(index-1);
				config.pieFeatures[i] = selectedFeature.getId();
			} else {
				config.pieFeatures[i] = -1;
			}
			int color = config.getColor(featureColors[i].getColorValue());
			config.featureColors[i] = color;
		}
		int index = sizeFeatureCombo.getSelectionIndex();
		if (index > 0) {
			Feature selectedFeature = allFeatures.get(index-1);
			config.sizeFeature = selectedFeature.getId();
		} else {
			config.sizeFeature = -1;
		}
		config.saveState(plate.getExperiment().getProtocol().getId(), layer.getId());
		layer.update();
		super.okPressed();
	}

	@Override
	public void applySettings(GridViewer viewer, IGridLayer layer) {
		((PieChartLayer)layer).update();
	}

}
