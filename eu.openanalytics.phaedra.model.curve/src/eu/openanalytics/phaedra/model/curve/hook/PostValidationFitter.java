package eu.openanalytics.phaedra.model.curve.hook;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.curve.Activator;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.validation.ValidationService;
import eu.openanalytics.phaedra.validation.hook.ValidationHookArguments;

/*
 * Triggers curve fitting for compounds where a sample was accepted/rejected.
 */
public class PostValidationFitter implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}
	
	@Override
	public void post(IHookArguments args) {
		ValidationHookArguments validationArgs = (ValidationHookArguments)args;
		
		if (!ValidationService.isWellAction(validationArgs.action)) return;
		
		List<Compound> compounds = new ArrayList<Compound>();
		for (Object item: validationArgs.objects) {
			if (item instanceof Well) {
				Well well = (Well)item;
				if (!PlateUtils.isControl(well) && well.getCompound() != null) {
					CollectionUtils.addUnique(compounds, well.getCompound());
				} else {
					// Control wells won't be fitted now. Instead, they will
					// be fitted by the PostCalculationFitter after plate
					// recalculation.
				}
			}
		}
		if (compounds.isEmpty()) return;
		
		List<Feature> features = CollectionUtils.findAll(ProtocolUtils.getFeatures(compounds.get(0)), CurveUtils.FEATURES_WITH_CURVES);
		for (Compound compound: compounds) {
			for (Feature feature: features) {
				try {
					CurveFitService.getInstance().fitCurves(compound, feature);
				} catch (CurveFitException e) {
					EclipseLog.error("Fit failed: " + e.getMessage(), e, Activator.getDefault());
				}
			}
		}
		
	}

}
