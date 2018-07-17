package eu.openanalytics.phaedra.ui.silo.cmd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.ui.protocol.dialog.FeatureSelectionDialog;
import eu.openanalytics.phaedra.ui.silo.Activator;

public class AddFeatureColumns extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		SiloDataset dataset = SelectionUtils.getFirstObject(selection, SiloDataset.class);
		if (dataset == null) return null;
		
		if (dataset.getSilo().getType() == GroupType.WELL.getType()) {
			ISiloAccessor<Well> accessor = SiloService.getInstance().getSiloAccessor(dataset.getSilo());
			configureFeatures(accessor, Feature.class, dataset.getName());	
		} else {
			ISiloAccessor<SubWellItem> accessor = SiloService.getInstance().getSiloAccessor(dataset.getSilo());
			configureFeatures(accessor, SubWellFeature.class, dataset.getName());
		}
		
		return null;
	}
	
	protected <ENTITY extends PlatformObject, FEATURE extends IFeature> void configureFeatures(
			ISiloAccessor<ENTITY> accessor, Class<FEATURE> featureClass, String datasetName) {
		
		ProtocolClass pClass = accessor.getSilo().getProtocolClass();
		List<FEATURE> selectedFeatures = new ArrayList<>();
		List<String> selectedNormalizations = new ArrayList<>();

		// Open the Feature Selection dialog.
		FeatureSelectionDialog<FEATURE> dialog = new FeatureSelectionDialog<>(
				Display.getDefault().getActiveShell(), pClass, featureClass, selectedFeatures, selectedNormalizations, 0, Integer.MAX_VALUE);
		boolean proceed = (dialog.open() == Window.OK);
		if (!proceed) return;

		if (datasetName == null) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "No dataset selected", "Cannot add columns: no dataset selected");
			return;
		}

		Job addColumnsJob = new Job("Adding Columns") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Adding Columns", selectedFeatures.size());
				try {
					// Check for Features that need to be added to the Silo.
					for (int i=0; i<selectedFeatures.size(); i++) {
						if (monitor.isCanceled()) return Status.CANCEL_STATUS;
						FEATURE f = selectedFeatures.get(i);
						monitor.subTask("Adding column " + (i+1) + "/" + selectedFeatures.size() + ": " + f.getName() + "");
						//TODO support normalization
						String norm = selectedNormalizations.get(i);
						String colName = f.getName();
						accessor.createColumn(datasetName, colName, SiloUtils.getDataType(f));
						monitor.worked(1);
					}
				} catch (SiloException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to add columns", e);
				}

				monitor.done();
				return Status.OK_STATUS;
			}
		};
		addColumnsJob.setUser(true);
		addColumnsJob.schedule();
	}
	
}
