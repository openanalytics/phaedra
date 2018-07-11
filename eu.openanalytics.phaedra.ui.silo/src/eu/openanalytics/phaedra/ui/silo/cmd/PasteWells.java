package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class PasteWells extends AbstractPasteCommand<Well> {

	@Override
	protected boolean asNewDataset() {
		return false;
	}
	
	@Override
	protected String getObjectName() {
		return "well";
	}
	
	@Override
	protected List<Well> getItems(ISelection selection) {
		return SelectionUtils.getObjects(selection, Well.class);
	}
}