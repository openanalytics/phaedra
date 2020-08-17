package eu.openanalytics.phaedra.internal.model.plate;

import org.eclipse.core.expressions.PropertyTester;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;


public class ExperimentPropertyTester extends PropertyTester {
	
	
	public ExperimentPropertyTester() {
	}
	
	
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (receiver instanceof Experiment) {
			final Experiment experiment = (Experiment)receiver;
			if (property.equals("isExperimentOpen")) { //$NON-NLS-1$
				return !experiment.isClosed();
			}
			if (property.equals("isExperimentClosed")) { //$NON-NLS-1$
				return experiment.isClosed();
			}
			return false;
		}
		return false;
	}

}
