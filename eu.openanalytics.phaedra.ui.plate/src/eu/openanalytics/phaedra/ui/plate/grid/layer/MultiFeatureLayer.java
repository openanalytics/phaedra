package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.primitives.Doubles;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.gridviewer.GridViewer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.IGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.ILayerConfigDialog;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.plate.grid.layer.config.MultiFeatureConfig;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class MultiFeatureLayer extends PlatesLayer {

	public final static String FIXED_PIE_SIZE = "FIXED_PIE_SIZE";

	private List<Plate> plates;

	private List<Feature> allFeatures;
	private List<Feature> features;
	private Feature currentFeature;
	private String currentStat;

	private Map<String, double[]> featureValues;
	private Map<String, IColorMethod> featureColors;
	private Map<String, Double> minValues;
	private Map<String, Double> maxValues;

	private long protocolId;

	private int minSize;
	private boolean fixedPieSize = true;

	private IModelEventListener modelEventListener;

	public MultiFeatureLayer() {
		modelEventListener = event -> {
			if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
				boolean updateRequired = false;
				ProtocolClass pc = (ProtocolClass)event.source;
				if (features != null) {
					for (Feature f: features) {
						if (f != null && f.getProtocolClass().equals(pc)) updateRequired = true;
					}
				}
				if (currentFeature != null && currentFeature.getProtocolClass().equals(pc)) updateRequired = true;
				if (updateRequired) update(true);
			}
		};
	}

	@Override
	public String getName() {
		return "Multi-feature heatmap";
	}

	@Override
	protected void doInitialize() {

		ModelEventService.getInstance().addEventListener(modelEventListener);

		plates = getPlates();
		allFeatures = PlateUtils.getFeatures(plates.get(0));
		allFeatures = CollectionUtils.findAll(allFeatures, ProtocolUtils.NUMERIC_FEATURES);

		protocolId = getPlates().get(0).getExperiment().getProtocol().getId();

		featureValues = new HashMap<>();
		minValues = new HashMap<>();
		maxValues = new HashMap<>();
		featureColors = new HashMap<>();

		if (features == null || features.isEmpty()) {
			features = new ArrayList<Feature>();
			loadDefaults();
		}
		update(true);
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasPlates() || !isEnabled()) return;

		update(false);
	}

	@Override
	public void dispose() {
		if (modelEventListener != null) ModelEventService.getInstance().removeEventListener(modelEventListener);
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new MultiFeatureHeatmapRenderer();
	}

	@Override
	public boolean hasConfigDialog() {
		return true;
	}

	@Override
	public ILayerConfigDialog createConfigDialog(Shell shell) {
		return new ConfigDialog(shell);
	}

	@Override
	public void setConfig(Object config) {
		features = ((MultiFeatureConfig) config).getFeatures();
		fixedPieSize = ((MultiFeatureConfig) config).fixedPieSize;
		currentFeature = getCurrentFeature();
	}

	@Override
	public Object getConfig() {
		return new MultiFeatureConfig(features, fixedPieSize);
	}

	private void loadDefaults() {
		int featureCount = 5;

		String fixedSize = GridState.getStringValue(protocolId, getId(), FIXED_PIE_SIZE);
		fixedPieSize = Boolean.valueOf(fixedSize);

		for (int i = 0; i < featureCount; i++) {
			String setting = GridState.getStringValue(protocolId, getId(), i + "");
			if (setting != null && !setting.isEmpty()) {
				int fId = Integer.parseInt(setting);
				if (fId == -1) {
					features.add(null);
				} else {
					features.add(allFeatures.get(fId));
				}
			}
		}

		minSize = Activator.getDefault().getPreferenceStore().getInt(Prefs.MULTI_FEATURE_MIN_SIZE);
	}

	private void update(boolean reloadRequired) {
		if (currentFeature == null || !currentFeature.equals(getCurrentFeature())) {
			reloadRequired = true;
			currentFeature = getCurrentFeature();
		}

		if (currentStat == null || !currentStat.equals(getLayerSupport().getStat())) {
			reloadRequired = true;
			currentStat = getLayerSupport().getStat();
		}

		if (reloadRequired) {
			featureValues.clear();
			minValues.clear();
			maxValues.clear();
			featureColors.clear();

			int nrOfWells = plates.get(0).getWells().size();
			int nrOfPlates = plates.size();

			for (Feature f : features) {
				if (f == null) f = getCurrentFeature();
				double[] values = new double[nrOfWells];
				List<Double> allValues = new ArrayList<>();

				for (int i = 0; i < nrOfWells; i++) {
					double[] tempValues = new double[nrOfPlates];
					for (int j = 0; j < nrOfPlates; j++) {
						if (plates.get(j).getWells().size() < i) {
							// This plate is smaller than the previous one, fill with NaNs.
							tempValues[j] = Double.NaN;
							continue;
						}
						Well well = PlateUtils.getWell(plates.get(j), i+1);
						if (well.getStatus() >= 0 || nrOfWells == 1) {
							tempValues[j] = CalculationService.getInstance().getAccessor(plates.get(j)).getNumericValue(well, f, f.getNormalization());
						} else{
							tempValues[j] = Double.NaN;
						}
					}
					values[i] = StatService.getInstance().calculate(getLayerSupport().getStat(), tempValues);
					allValues.add(values[i]);
				}

				featureValues.put(f.getName(), values);
				minValues.put(f.getName(), StatService.getInstance().calculate("min", Doubles.toArray(allValues)));
				maxValues.put(f.getName(), StatService.getInstance().calculate("max", Doubles.toArray(allValues)));
				IColorMethod colorMethod = ColorMethodFactory.createColorMethod(f);
				colorMethod.initialize(ColorMethodFactory.createData(plates, f, f.getNormalization(), getLayerSupport().getStat()));
				featureColors.put(f.getName(), colorMethod);
			}
		}
	}

	private void setFeatures(int[] indices) {
		features.clear();
		GridState.saveValue(protocolId, getId(), FIXED_PIE_SIZE, fixedPieSize + "");
		for (int i = 0; i < indices.length; i++) {
			int index = indices[i];

			if (index > 1) {
				features.add(allFeatures.get(index - 2));
				GridState.saveValue(protocolId, getId(), i + "", index-2 + "");
			} else if (index > 0) {
				features.add(null);
				GridState.saveValue(protocolId, getId(), i + "", -1 + "");
			} else {
				GridState.removeValue(protocolId, getId(), i + "");
			}

		}
	}
	
	private Feature getCurrentFeature() {
		IFeatureProvider provider = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider"));
		if (provider == null) provider = ProtocolUIService.getInstance();
		return provider.getCurrentFeature();
	}

	private class MultiFeatureHeatmapRenderer extends BaseGridCellRenderer {

		private ColorStore colorStore;

		public MultiFeatureHeatmapRenderer() {
			colorStore = new ColorStore();
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;

			List<Well> wells = getWells(cell.getData());

			int featureCount = features.size();
			if (wells == null || wells.isEmpty() || featureCount == 0) return;

			int antiAliasState = gc.getAntialias();
			gc.setAntialias(SWT.ON);

			// Add a 1 pixel margin to prevent border collision.
			x += 1;
			y += 1;
			w -= 2;
			h -= 2;

			int availableW = w - minSize;
			int availableH = h - minSize;
			if (availableW < 0 || availableH < 0) {
				availableW = w;
				availableH = h;
				minSize = 0;
			}

			int degreesPerFeature = 360 / featureCount;
			for (int i = 0; i < featureCount; i++) {
				Feature f = features.get(i);
				if (f == null) f = getCurrentFeature();
				if (!featureValues.containsKey(f.getName())) continue;

				Well well = wells.get(0);
				double value = featureValues.get(f.getName())[NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns())-1];
				double minValue = minValues.get(f.getName());
				double maxValue = maxValues.get(f.getName());

				int startArc = i * degreesPerFeature;

				int newW = w;
				int newH = h;
				int newX = x;
				int newY = y;

				if (!fixedPieSize && !Double.isInfinite(value)) {
					if (value < minValue)
						value = minValue;
					if (value > maxValue)
						value = maxValue;
					double normValue = (value - minValue) / (maxValue - minValue);

					newW = (int)(normValue * availableW) - 1 + minSize;
					newH = (int)(normValue * availableH) - 1 + minSize;
					newX = (int)(x + (1 - normValue) * availableW / 2);
					newY = (int)(y + (1 - normValue) * availableH / 2);
				}

				RGB color = new RGB(150, 150, 150);
				RGB lookupColor = featureColors.get(f.getName()).getColor(value);
				if (!Double.isNaN(value) && lookupColor != null) color = lookupColor;
				Color bgColor = colorStore.get(color);
				gc.setBackground(bgColor);
				gc.fillArc(newX, newY, newW + 1, newH + 1, startArc, degreesPerFeature);

				gc.setForeground(colorStore.get(new RGB(0, 0, 0)));
				gc.setLineWidth(1);
				gc.drawArc(newX, newY, newW, newH, startArc, degreesPerFeature);

				if (featureCount > 1) {
					double angle = (i * degreesPerFeature) / 180.0 * Math.PI;
					int x2 = newX + (newW / 2) + (int) (Math.cos(angle) * newW / 2);
					int y2 = newY + (newH / 2) - (int) (Math.sin(angle) * newH / 2);
					gc.drawLine(newX + (newW / 2), newY + (newH / 2), x2, y2);

					angle = ((i + 1) * degreesPerFeature) / 180.0 * Math.PI;
					int x3 = newX + (newW / 2) + (int) (Math.cos(angle) * newW / 2);
					int y3 = newY + (newH / 2) - (int) (Math.sin(angle) * newH / 2);
					gc.drawLine(newX + (newW / 2), newY + (newH / 2), x3, y3);
				}
			}

			if (featureCount > 1 && fixedPieSize) {
				gc.drawLine(x + (w / 2), y + (h / 2), x + w, y + (h / 2));
			}

			gc.setAntialias(antiAliasState);
		}

		@Override
		public void dispose() {
			colorStore.dispose();
		}
	}

	private class ConfigDialog extends TitleAreaDialog implements ILayerConfigDialog {

		private Combo[] featureCombos;
		private Button fixedPieSizeBtn;

		private int[] selectedFeatures;
		private boolean useFixedPieSize;

		public ConfigDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Configuration: " + getName());
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
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

			Label lbl = null;
			List<String> featureNames = CollectionUtils.transform(allFeatures, ProtocolUtils.FEATURE_NAMES);
			String[] nameArray = new String[featureNames.size() + 2];
			nameArray[0] = "<None>";
			nameArray[1] = "Current Feature";
			for (int i = 0; i < featureNames.size(); i++) nameArray[i + 2] = featureNames.get(i);

			int featureCount = 5;
			featureCombos = new Combo[featureCount];

			for (int i = 0; i < featureCount; i++) {
				lbl = new Label(container, SWT.NONE);
				lbl.setText("Feature " + (i + 1) + ":");

				featureCombos[i] = new Combo(container, SWT.READ_ONLY);
				featureCombos[i].setItems(nameArray);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(featureCombos[i]);

				String feature = GridState.getStringValue(protocolId, getId(), i + "");
				if (feature == null || feature == "") featureCombos[i].select(0);
				else {
					int index = Integer.parseInt(feature);
					if (index >= 0) featureCombos[i].select(index+2);
					if (index == -1) featureCombos[i].select(1);
				}
			}

			fixedPieSizeBtn = new Button(container, SWT.CHECK);
			fixedPieSizeBtn.setText("Use fixed pie size");
			fixedPieSizeBtn.setSelection(fixedPieSize);

			setTitle(getName());
			setMessage("Select the features to display.");
			return area;
		}

		@Override
		protected void okPressed() {
			selectedFeatures = new int[featureCombos.length];
			for (int i = 0; i < featureCombos.length; i++) {
				selectedFeatures[i] = featureCombos[i].getSelectionIndex();
			}
			useFixedPieSize = fixedPieSizeBtn.getSelection();
			applySettings(getLayerSupport().getViewer(), MultiFeatureLayer.this);
			super.okPressed();
		}

		@Override
		public void applySettings(GridViewer viewer, IGridLayer layer) {
			((MultiFeatureLayer)layer).setFeatures(selectedFeatures);
			((MultiFeatureLayer)layer).update(true);
			((MultiFeatureLayer)layer).fixedPieSize = useFixedPieSize;
			viewer.getGrid().redraw();
		}
	}

}