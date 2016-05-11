package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

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

				Set<Compound> compounds = new HashSet<Compound>();

				boolean noParent = false;
				if (selection instanceof ConfigurableStructuredSelection) {
					noParent = ((ConfigurableStructuredSelection) selection).hasConfig(ConfigurableStructuredSelection.NO_PARENT);
				}

				Object sample = selection.getFirstElement();
				if (sample instanceof Experiment) {
					List<Experiment> experiments = SelectionUtils.getObjects(selection, Experiment.class);
					final List<Plate> plates = new ArrayList<>();
					// Optimization: leave out the single-dose plates.
					plates.addAll(CurveService.getInstance().getPlatesWithCurves(experiments));
					
					for (Plate plate: plates) {
						for (Compound c: plate.getCompounds()) add(c, compounds);
						monitor.worked(1);
					}
				} else if (sample instanceof Plate) {
					List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
					for (Plate plate: plates) {
						for (Compound c: plate.getCompounds()) add(c, compounds);
						monitor.worked(1);
					}
				} else {
					// Attempt to use adapters
					for (Iterator<?> it = selection.iterator(); it.hasNext();) {
						Object o = it.next();
						if (o instanceof Well) {
							if (noParent) {
								add(((Well)o).getCompound(), compounds);
							} else {
								Plate plate = ((Well) o).getPlate();
								for (Compound c: plate.getCompounds()) add(c, compounds);
							}
						} else {
							add(SelectionUtils.getAsClass(o, Compound.class), compounds);
						}
						monitor.worked(1);
					}
				}

				// Pre-load the curves of the first few compounds to prevent UI block.
				List<Compound> compoundList = new ArrayList<>(compounds);
				if (!compoundList.isEmpty()) {
					List<Feature> allFeatures = PlateUtils.getFeatures(compoundList.get(0).getPlate());
					List<Feature> curveFeatures = new ArrayList<Feature>();
					for (Feature f: allFeatures) {
						String kind = f.getCurveSettings().get(CurveSettings.KIND);
						if (f.isKey() && kind != null && !kind.isEmpty()) curveFeatures.add(f);
					}
					for (int i=0; i<10; i++) {
						if (i>= compoundList.size()) break;
						for (Feature f: curveFeatures) {
							Compound c = compoundList.get(i);
							for (Well w: c.getWells()) CurveService.getInstance().getCurve(w, f);
						}
					}
				}

				if (compounds.isEmpty()) {
					Display.getDefault().asyncExec(() -> {
						String msg = "The selected plates do not contain any dose-response curves.";
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No data", msg);
					});
				}

				Display.getDefault().asyncExec(() -> EditorFactory.getInstance().openEditor(compoundList ));
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		loadCompoundsJob.setUser(true);
		loadCompoundsJob.schedule();
	}

	private void add(Compound c, Set<Compound> compounds) {
		if (c != null && c.getWells().size() >= CurveService.MIN_SAMPLES_FOR_FIT) compounds.add(c);
	}

}