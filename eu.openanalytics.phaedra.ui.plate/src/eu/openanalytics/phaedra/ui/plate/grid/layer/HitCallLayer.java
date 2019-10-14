package eu.openanalytics.phaedra.ui.plate.grid.layer;

import org.eclipse.swt.graphics.GC;

import eu.openanalytics.phaedra.base.ui.gridviewer.widget.GridCell;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.BaseGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.gridviewer.widget.render.IGridCellRenderer;
import eu.openanalytics.phaedra.base.ui.util.misc.ColorCache;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.grid.PlatesLayer;
import eu.openanalytics.phaedra.ui.protocol.calculation.RulesetRenderStyle;
import eu.openanalytics.phaedra.ui.protocol.provider.IFeatureProvider;

public class HitCallLayer extends PlatesLayer {

	private Plate plate;
	private Feature currentFeature;
	private FormulaRuleset currentRuleset;
	
	@Override
	public String getName() {
		return "Hit Calling";
	}
	
	@Override
	protected void doInitialize() {
		if (hasPlates()) plate = getPlate();
		currentFeature = null;
		update(null, null);
	}
	
	@Override
	public IGridCellRenderer createRenderer() {
		return new HitCallRenderer();
	}
	
	@Override
	public void update(GridCell cell, Object modelObject) {
		if (!hasPlates() || !isEnabled()) return;
		
		IFeatureProvider provider = ((IFeatureProvider) getLayerSupport().getAttribute("featureProvider"));
		Feature feature = provider.getCurrentFeature();
		if (!feature.equals(currentFeature)) {
			currentFeature = feature;
			currentRuleset = FormulaService.getInstance().getRulesetForFeature(currentFeature.getId(), RulesetType.HitCalling.getCode());
		}
	}
	
	private class HitCallRenderer extends BaseGridCellRenderer {
		
		@Override
		public void render(GridCell cell, GC gc, int x, int y, int w, int h) {
			if (!isEnabled() || !hasPlates() || currentFeature == null) return;
			if (currentRuleset == null || !currentRuleset.isShowInUI()) return;
			
			Well well = (Well) cell.getData();
			int wellNr = PlateUtils.getWellNr(well);
			
			double[] hits = HitCallService.getInstance().getHitValues(plate, currentFeature);
			if (hits == null || hits.length <= wellNr) return;
			
			double hitValue = hits[wellNr - 1];
			if (!Double.isNaN(hitValue) && hitValue > 0) {
				RulesetRenderStyle style = RulesetRenderStyle.getByCode(currentRuleset.getStyle());
				gc.setForeground(ColorCache.get(currentRuleset.getColor()));
				gc.setLineWidth(2);
				style.renderImage(gc, x+(w/2), y+(h/2), w/4);
			}
		}
	}
}
