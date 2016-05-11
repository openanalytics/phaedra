package eu.openanalytics.phaedra.ui.silo.util;

import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.silo.Activator;

public class SiloPasteSettings {

	public enum WellType {
		ALL
		, CONTROL
		, SAMPLE
	}

	private boolean newGroup;
	private WellType supportedWellType;
	private boolean isIncludeRejected;
	private Double subsetPct;

	public SiloPasteSettings() {
		this.newGroup = false;
		this.supportedWellType = WellType.ALL;
		this.isIncludeRejected = false;
		this.subsetPct = null;
	}

	public boolean isNewGroup() {
		return newGroup;
	}

	public void setNewGroup(boolean newGroup) {
		this.newGroup = newGroup;
	}

	public WellType getSupportedWellType() {
		return supportedWellType;
	}

	public void setSupportedWellType(WellType supportedWellType) {
		this.supportedWellType = supportedWellType;
	}

	public boolean isIncludeRejected() {
		return isIncludeRejected;
	}

	public void setIncludeRejected(boolean isIncludeRejected) {
		this.isIncludeRejected = isIncludeRejected;
	}

	/**
	 * The percentage of how much data to include.
	 * Can be 'null' if no subsetting has to be done.
	 * 
	 * @return
	 */
	public Double getSubsetPct() {
		return subsetPct;
	}

	public void setSubsetPct(Double subsetPct) {
		this.subsetPct = subsetPct;
	}

	/*
	 * *******************
	 * Convenience Methods
	 * *******************
	 */

	public boolean isValidWell(Well well) {
		boolean valid = true;
		valid = valid && (well.getStatus() > -1 || well.getStatus() < 0 && isIncludeRejected);
		switch (supportedWellType) {
		case ALL:
			// Do nothing.
			break;
		case CONTROL:
			valid = valid && PlateUtils.isControl(well);
			break;
		case SAMPLE:
			valid = valid && PlateUtils.isSample(well);
			break;
		default:
			Activator.getDefault().getLog().log(
					new Status(Status.WARNING, Activator.PLUGIN_ID, "Unsupported Well Group Type for Silo Paste Settings."));
			break;
		}

		return valid;
	}

}