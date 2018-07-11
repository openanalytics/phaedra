package eu.openanalytics.phaedra.ui.silo.cmd;


public class PasteWellsAsNewGroup extends PasteWells {

	@Override
	protected boolean asNewDataset() {
		return true;
	}
}
