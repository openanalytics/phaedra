package eu.openanalytics.phaedra.base.ui.util.pinning;

import java.util.List;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.StructuredSelection;

public class ConfigurableStructuredSelection extends StructuredSelection {

	// No setting.
	public static final int NONE = 0;
	// The selection has an obvious parent which can be used, e.g. Well selection from QuickHeatmap has one parent Plate.
	public static final int USE_PARENT = 1 << 1;
	// The selection is arbitrary, e.g. Well selection from Query has multiple parents.
	public static final int NO_PARENT = 1 << 2;

	private int configuration;

	public ConfigurableStructuredSelection() {
		super();
	}

	public ConfigurableStructuredSelection(Object element) {
		super(element);
	}

	public ConfigurableStructuredSelection(Object[] elements) {
		super(elements);
	}

	public ConfigurableStructuredSelection(List<?> list) {
		super(list);
	}

	public ConfigurableStructuredSelection(List<?> list, IElementComparer comparer) {
		super(list, comparer);
	}

	public void setConfiguration(int configuration) {
		this.configuration = configuration;
	}

	public int getConfiguration() {
		return configuration;
	}

	public boolean hasConfig(int config) {
		return (configuration & config) == config;
	}

}
