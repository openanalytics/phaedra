package eu.openanalytics.phaedra.ui.plate.grid.layer;

import java.util.List;
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
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.calculation.RulesetRenderStyle;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;

public class OutlierDetectionLayer extends PlatesLayer {

	private Plate plate;
	private ProtocolClass currentPClass;
	private List<FormulaRuleset> currentRulesets;
	private List<Well> currentOutliers;
	
	@Override
	public String getName() {
		return "Outlier Detection";
	}
	
	@Override
	protected void doInitialize() {
		if (hasPlates()) plate = getPlate();
		currentPClass = null;
		currentRulesets = null;
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
			currentRulesets = FormulaService.getInstance()
					.getRulesetsForProtocolClass(pClass.getId(), RulesetType.OutlierDetection.getCode())
					.values().stream().collect(Collectors.toList());
			currentOutliers = PlateService.streamableList(plate.getWells()).stream()
				.filter(w -> {
					int wellNr = PlateUtils.getWellNr(w);
					for (FormulaRuleset rs: currentRulesets) {
						boolean[] outliers = OutlierDetectionService.getInstance().getOutliers(w.getPlate(), rs.getFeature());
						if (outliers[wellNr - 1]) return true;
					}
					return false;
				}).collect(Collectors.toList());
		}
	}
	
	private class OutlierDetectionRenderer extends BaseGridCellRenderer {
		
		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates() || currentPClass == null) return;
			if (currentOutliers == null || currentOutliers.isEmpty()) return;
			
			Well well = (Well) cell.getData();
			if (well.getStatus() >= 0 && currentOutliers.contains(well)) {
				FormulaRuleset ruleset = currentRulesets.get(0);
				RulesetRenderStyle style = RulesetRenderStyle.getByCode(ruleset.getStyle());
				gc.setForeground(ColorCache.get(ruleset.getColor()));
				gc.setLineWidth(2);
				style.renderImage(gc, x+(w/2), y+(h/2), w/4);
			}
		}
	}
}
