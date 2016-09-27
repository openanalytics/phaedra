package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class BrowseCompounds extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// Obtain a selection from the active part.
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		ISelection selection = page.getSelection();

		// If no selection found, try the event data.
		if (selection == null || !(selection instanceof IStructuredSelection)) {
			if (event.getTrigger() instanceof Event) {
				Object data = ((Event)event.getTrigger()).data;
				if (data instanceof List) selection = new StructuredSelection((List<?>)data);
			}
		}

		if (selection == null || !(selection instanceof IStructuredSelection)) {
			// Still no valid selection: abort.
			return null;
		}

		loadCompounds((IStructuredSelection)selection);
		return null;
	}

	public static void execute(IStructuredSelection selection) {
		new BrowseCompounds().loadCompounds(selection);
	}

	private void loadCompounds(IStructuredSelection selection) {

		Job loadCompoundsJob = new Job("Loading Compounds") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading Compounds", IProgressMonitor.UNKNOWN);

				boolean noParent = false;
				if (selection instanceof ConfigurableStructuredSelection) {
					noParent = ((ConfigurableStructuredSelection) selection).hasConfig(ConfigurableStructuredSelection.NO_PARENT);
				}

				Object sample = selection.getFirstElement();
				Stream<Compound> compoundStream = null;
				if (sample instanceof Experiment) {
					compoundStream = SelectionUtils.getObjects(selection, Experiment.class).stream()
						.flatMap(e -> PlateService.streamableList(PlateService.getInstance().getPlates(e)).stream())
						.flatMap(p -> PlateService.streamableList(p.getCompounds()).stream());
				} else if (sample instanceof Plate) {
					compoundStream = SelectionUtils.getObjects(selection, Plate.class).stream()
							.flatMap(p -> PlateService.streamableList(p.getCompounds()).stream());
				} else if (sample instanceof Well && noParent) {
					compoundStream = SelectionUtils.getObjects(selection, Well.class).stream()
							.map(w -> w.getCompound());
				} else if (sample instanceof Well) {
					compoundStream = SelectionUtils.getObjects(selection, Well.class).stream()
							.map(w -> w.getPlate())
							.flatMap(p -> PlateService.streamableList(p.getCompounds()).stream());
				} else {
					compoundStream = SelectionUtils.getObjects(selection, Compound.class, true).stream();
				}
				Set<Compound> compounds = compoundStream.collect(Collectors.toSet());
				
				if (compounds.isEmpty()) {
					Display.getDefault().asyncExec(() -> {
						String msg = "The selected plates do not contain any dose-response curves.";
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No data", msg);
					});
				}

				Display.getDefault().asyncExec(() -> EditorFactory.getInstance().openEditor(new ArrayList<>(compounds)));
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		loadCompoundsJob.setUser(true);
		loadCompoundsJob.schedule();
	}

}