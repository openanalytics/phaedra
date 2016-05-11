package eu.openanalytics.phaedra.ui.plate.grid.correlationmatrix;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;

public abstract class FeatureEntityLayer<ENTITY, FEATURE> extends BaseGridLayer {

	public static final String FILL = "Fill";
	public static final String TOP_RIGHT = "Top Right";
	public static final String BOTTOM_LEFT = "Bottom Left";
	public static final String DIAGONAL = "Diagonal";
	public static final String EXCLUDE_GIAGONAL = "Exclude Diagonal";
	public static final String[] FILL_OPTIONS = new String[] {FILL, TOP_RIGHT, BOTTOM_LEFT, DIAGONAL, EXCLUDE_GIAGONAL};

	private List<ENTITY> entities;
	private List<FEATURE> features;

	private String fillOption;

	protected List<FEATURE> getFeatures() {
		return features;
	}

	protected void setFeatures(List<FEATURE> features) {
		this.features = features;
	}

	protected List<ENTITY> getEntities() {
		return entities;
	}

	protected void setEntities(List<ENTITY> entities) {
		this.entities = entities;
	}

	protected boolean hasSelection() {
		return (entities != null && !entities.isEmpty());
	}

	protected boolean hasFeatures() {
		return (features != null && !features.isEmpty());
	}

	@Override
	protected void initialize() {
		// Make sure to clean up resources from previous initializations.
		dispose();
		// Do not initialize if new input is null.
		if (hasSelection()) doInitialize();
	}

	protected abstract void doInitialize();
	
	protected abstract boolean getPreference();

	@Override
	public boolean isDefaultEnabled() {
		Boolean defaultEnabled = GridState.getBooleanValue(GridState.ALL_PROTOCOLS, getId(), GridState.DEFAULT_ENABLED);
		if (defaultEnabled != null) {
			return defaultEnabled;
		} else {
			return getPreference();
		}
	}

	public void setFillOption(String fillOption) {
		this.fillOption = fillOption;
	}

	public String getFillOption() {
		return fillOption;
	}

	public String getDefaultFillOption() {
		return FILL;
	}

}