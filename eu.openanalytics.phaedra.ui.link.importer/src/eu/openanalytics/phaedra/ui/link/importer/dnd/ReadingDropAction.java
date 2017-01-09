package eu.openanalytics.phaedra.ui.link.importer.dnd;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.IDropActionDelegate;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkService;
import eu.openanalytics.phaedra.link.data.DataLinkTask;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;


public class ReadingDropAction implements IDropActionDelegate {

	@Override
	public boolean run(Object source, Object target) {
		// Note: this extension works on PluginTransfer but we are using LocalSelectionTransfer nonetheless.
		// Because PluginTransfer requires serializing the object to byte[], and that's overkill here.
		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		Experiment experiment = SelectionUtils.getAsClass(target, Experiment.class);
		
		String message = "Are you sure you want to import these " + selection.size() + " reading(s) as new plates into experiment\n" + experiment + "?";
		boolean confirmed = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Import Readings", message);
		
		if (confirmed) {
			// Create and submit a data link task.
			final DataLinkTask task = DataLinkService.getInstance().createTask();
			for (Object o: selection.toArray()) {
				task.selectedReadings.add((PlateReading)o);
			}
			task.targetExperiment = experiment;
			DataLinkService.getInstance().executeTaskInJob(task);
		}
		
		return true;
	}
}
