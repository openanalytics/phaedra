package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.misc.ColorCache;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.calculation.outlier.OutlierDetectionService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.calculation.RulesetRenderStyle;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;

/**
 * Displays an outlier symbol on each well that matches the outlier ruleset of at least 1 feature.
 */
public class OutlierDetectionLayer extends PlatesLayer {

	private Plate plate;
	private ProtocolClass currentPClass;
	private Map<Well, FormulaRuleset> currentOutliers;
	
	@Override
	public String getName() {
		return "Outlier Detection";
	}
	
	@Override
	protected void doInitialize() {
		if (hasPlates()) plate = getPlate();
		currentPClass = null;
		currentOutliers = null;
		currentOutliers = null;
		update(null, null);
	}
	
	@Override
	public IGridCellRenderer createRenderer() {
		return new OutlierDetectionRenderer();
	}
	
	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasPlates() || !isEnabled()) return;
		
		IFeatureProvider provider = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider"));
		ProtocolClass pClass = provider.getCurrentProtocolClass();
		if (!pClass.equals(currentPClass)) {
			currentPClass = pClass;
			currentOutliers = new HashMap<>();
			List<FormulaRuleset> rulesets = FormulaService.getInstance()
					.getRulesetsForProtocolClass(pClass.getId(), RulesetType.OutlierDetection.getCode())
					.values().stream().collect(Collectors.toList());
			
			for (FormulaRuleset rs: rulesets) {
				if (!rs.isShowInUI()) continue;
				boolean[] outliers = OutlierDetectionService.getInstance().getOutliers(plate, rs.getFeature());
				for (Well well: plate.getWells()) {
					if (currentOutliers.containsKey(well)) continue;
					int wellNr = PlateUtils.getWellNr(well);
					if (outliers[wellNr - 1]) currentOutliers.put(well, rs);
				}
			}
		}
	}
	
	private class OutlierDetectionRenderer extends BaseGridCellRenderer {
		
		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates() || currentPClass == null) return;
			if (currentOutliers == null || currentOutliers.isEmpty()) return;
			
			Well well = (Well) cell.getData();
			FormulaRuleset ruleset = currentOutliers.get(well);
			if (well.getStatus() >= 0 && ruleset != null) {
				RulesetRenderStyle style = RulesetRenderStyle.getByCode(ruleset.getStyle());
				gc.setForeground(ColorCache.get(ruleset.getColor()));
				gc.setLineWidth(2);
				style.renderImage(gc, x+(w/2), y+(h/2), w/4);
			}
		}
	}
}
