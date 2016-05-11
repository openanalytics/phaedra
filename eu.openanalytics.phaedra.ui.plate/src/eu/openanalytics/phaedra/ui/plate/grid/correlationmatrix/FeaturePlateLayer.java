package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import org.apache.commons.math3.linear.RealMatrix;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public abstract class FeaturePlateLayer extends FeatureEntityLayer<Well, Feature> {

	private RealMatrix matrix;

	@Override
	public void setInput(Object newInput) {
		if (newInput instanceof FeatureInput) {
			FeatureInput featureInput = (FeatureInput) newInput;
			setEntities(featureInput.getCurrentWells());
			setFeatures(featureInput.getSelectedFeatures());

			matrix = featureInput.getMatrix();
		}
		super.setInput(newInput);
	}

	@Override
	protected boolean getPreference() {
		return Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SHOW_DEFAULT + getClass());
	}

	public RealMatrix getMatrix() {
		return matrix;
	}

}