package eu.openanalytics.phaedra.ui.link.importer.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.ui.part.PluginTransferData;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.ui.link.importer.cmd.DeleteReadings;
import eu.openanalytics.phaedra.ui.link.importer.dnd.ReadingDropAction;


public class ReadingHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith("new.readings.reading."));
	}

	@Override
	public void createContextMenu(IElement[] elements, IMenuManager mgr) {
		Action action = new Action("Delete Reading(s)", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				handleDeleteReadings(elements);
			}
		};
		action.setImageDescriptor(IconManager.getIconDescriptor("plate_delete.png"));
		mgr.add(action);
	}

	@Override
	public void dragStart(IElement[] elements, DragSourceEvent event) {
		event.doit = true;
		event.detail = DND.DROP_COPY;
	}

	@Override
	public void dragSetData(IElement[] elements, DragSourceEvent event) {
		List<PlateReading> readings = new ArrayList<>();
		for (IElement e: elements) readings.add((PlateReading)e.getData());
		// Uses both LocalSelectionTransfer and PluginTransferData, see ReadingDropAction for more information.
		LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(readings));
		event.data = new PluginTransferData(ReadingDropAction.class.getName(), new byte[]{0});
	}

	private void handleDeleteReadings(IElement[] elements) {
		List<PlateReading> readings = new ArrayList<>();
		for (IElement el: elements) readings.add((PlateReading)el.getData());
		DeleteReadings.execute(readings);
	}
}
