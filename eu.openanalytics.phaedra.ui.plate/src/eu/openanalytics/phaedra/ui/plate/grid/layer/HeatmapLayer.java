package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethod;
import eu.openanalytics.phaedra.base.ui.colormethod.IColorMethodData;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.Grid;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.util.misc.ColorStore;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;
import eu.openanalytics.phaedra.ui.protocol.util.ColorMethodFactory;

public class HeatmapLayer extends PlatesLayer {

	private List<Plate> plates;
	private Feature currentFeature;
	private String currentNormalization;
	private boolean currentIsExpLimit;

	private IColorMethod colorMethod;
	private IColorMethodData colorMethodData;
	private IModelEventListener modelEventListener;

	public HeatmapLayer() {
		modelEventListener = event -> {
			boolean doUpdate = false;
			if (event.type == ModelEventType.Calculated && event.source instanceof Plate) {
				Plate plate = (Plate)event.source;
				if (plates.contains(plate)) doUpdate = true;
			} else if (event.type == ModelEventType.ObjectChanged && event.source instanceof ProtocolClass) {
				ProtocolClass pc = (ProtocolClass)event.source;
				ProtocolClass currentPc = PlateUtils.getProtocolClass(plates.get(0));
				if (currentPc.equals(pc)) doUpdate = true;
			}

			if (doUpdate) {
				update(currentFeature, currentNormalization, true);
				Display.getDefault().asyncExec(() -> {
					Grid grid = getLayerSupport().getViewer().getGrid();
					if (!grid.isDisposed()) grid.redraw();
				});
			}
		};
	}

	@Override
	public String getName() {
		return "Heatmap";
	}

	@Override
	protected void doInitialize() {
		plates = getPlates();
		ModelEventService.getInstance().addEventListener(modelEventListener);
		currentFeature = null;
		currentNormalization = null;
		update(null, null);
	}

	@Override
	public IGridCellRenderer createRenderer() {
		return new HeatmapRenderer();
	}

	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasPlates() || !isEnabled()) return;

		IFeatureProvider provider = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider"));
		Feature feature = provider.getCurrentFeature();
		String normalization = provider.getCurrentNormalization();
		boolean isExpLimit = provider.isExperimentLimit();
		if (feature == null) return;
		update(feature, normalization, !currentIsExpLimit == isExpLimit);
	}

	@Override
	public void dispose() {
		if (modelEventListener != null) ModelEventService.getInstance().removeEventListener(modelEventListener);
	}

	protected IColorMethod getColorMethod() {
		return colorMethod;
	}

	private void update(Feature feature, String normalization, boolean colorMethodStale) {
		if (!feature.equals(currentFeature)
				|| !normalization.equals(currentNormalization)) {
			colorMethodStale = true;
			currentFeature = feature;
			currentNormalization = normalization;
		}

		if (colorMethodStale) {
			colorMethod = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider")).getCurrentColorMethod();
			currentIsExpLimit = ProtocolUIService.getInstance().isExperimentLimit();
			if (currentIsExpLimit) {
				List<Experiment> exps = new ArrayList<>();
				for (Plate p : plates) exps.add(p.getExperiment());
				colorMethodData = ColorMethodFactory.createData(exps, currentFeature, currentNormalization, getLayerSupport().getStat(), multiPlate);
			} else {
				colorMethodData = ColorMethodFactory.createData(plates, currentFeature, currentNormalization, getLayerSupport().getStat());
			}
		}
	}

	private class HeatmapRenderer extends BaseGridCellRenderer {

		private ColorStore colorStore;

		public HeatmapRenderer() {
			colorStore = new ColorStore();
		}

		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates()) return;

			// Initialize on every render, because another plate may have set its own colorMethodData in the meantime.
			colorMethod.initialize(colorMethodData);
			RGB color = new RGB(150,150,150);

			List<Well> wells = getWells(cell.getData());
			if (wells.isEmpty()) return;
			
			if (currentFeature.isNumeric()) {
				double[] values = new double[wells.size()];
				int index = 0;
				if (currentFeature != null) {
					for (Well well : wells) {
						if (well != null && (well.getStatus() >= 0 || wells.size() == 1)) {
							values[index++] = CalculationService.getInstance().getAccessor(well.getPlate()).getNumericValue(well, currentFeature, currentNormalization);
						} else {
							values[index++] = Double.NaN;
						}
					}
				}
				double value = StatService.getInstance().calculate(getLayerSupport().getStat(), values);
				RGB lookupColor = colorMethod.getColor(value);
				if (!Double.isNaN(value) && lookupColor != null) color = lookupColor;
			} else {
				//Note: multi-well layers are not suported. The first well is taken.
				Well well = wells.get(0);
				String value = CalculationService.getInstance().getAccessor(well.getPlate()).getStringValue(well, currentFeature);
				RGB lookupColor = colorMethod.getColor(value);
				if (lookupColor != null) color = lookupColor;
			}

			Color bgColor = colorStore.get(color);
			gc.setBackground(bgColor);
			gc.fillRectangle(x,y,w+1,h+1);

			if (w > 3 && h > 3) {
				gc.setLineWidth(1);
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
				gc.drawRectangle(x,y,w,h);
			}
		}

		@Override
		public void dispose() {
			colorStore.dispose();
		}

	}
}
