package eu.openanalytics.phaedra.ui.plate.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;


public class ExperimentContentProvider implements IStructuredContentProvider, ITreeContentProvider {
	
	
	private boolean excludeClosed;
	
	
	public ExperimentContentProvider() {
	}
	
	public ExperimentContentProvider(final boolean excludeClosed) {
		this.excludeClosed = excludeClosed;
	}

	@Override
	public void dispose() {
		// Nothing to dispose.
	}
	
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Do nothing.
	}

	@Override
	public Object getParent(Object child) {
		if (child instanceof Protocol) {
			return "root";
		} else if (child instanceof Experiment) {
			return ((Experiment) child).getProtocol();
		}
		return null;
	}
	
	@Override
	public boolean hasChildren(Object parent) {
		if (parent instanceof String) {
			return true;
		} else if (parent instanceof Protocol) {
			final Protocol protocol = (Protocol)parent;
			final List<Experiment> experiments = PlateService.getInstance()
					.getExperiments(protocol, this.excludeClosed );
			return (!experiments.isEmpty());
		}
		return false;
	}
	
	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof String) {
			List<Protocol> protocols = ProtocolService.getInstance().getProtocols();
			return protocols.toArray();
		} else if (parent instanceof Protocol) {
			final Protocol protocol = (Protocol)parent;
			final List<Experiment> experiments = PlateService.getInstance()
					.getExperiments(protocol, this.excludeClosed );
			final Experiment[] array = experiments.toArray(new Experiment[experiments.size()]);
			Arrays.sort(array, PlateUtils.EXPERIMENT_NAME_SORTER);
			return array;
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
}
