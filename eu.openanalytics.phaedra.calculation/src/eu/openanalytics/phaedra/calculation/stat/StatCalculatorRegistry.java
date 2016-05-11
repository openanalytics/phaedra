package eu.openanalytics.phaedra.calculation.stat;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.calculation.stat.impl.BaseStatCalculator;

public class StatCalculatorRegistry {

	private Map<String, IStatCalculator> calculators;
	
	public StatCalculatorRegistry() {
		calculators = new HashMap<String, IStatCalculator>();
		loadCalculators();
	}
	
	private void loadCalculators() {
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IStatCalculator.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IStatCalculator.ATTR_CLASS);
				if (o instanceof IStatCalculator) {
					IStatCalculator calculator = (IStatCalculator)o;
					String name = el.getAttribute(IStatCalculator.ATTR_NAME);
					if (calculator instanceof BaseStatCalculator) {
						BaseStatCalculator baseCalculator = (BaseStatCalculator)calculator;
						baseCalculator.setName(name);
					}
					calculators.put(name, calculator);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
	}
	
	public String[] getCalculatorNames() {
		return calculators.keySet().toArray(new String[calculators.size()]);
	}
	
	public IStatCalculator getCalculator(String name) {
		return calculators.get(name);
	}
}
