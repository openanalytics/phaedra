package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolContentProvider implements IStructuredContentProvider, ITreeContentProvider {

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
		if (child instanceof ProtocolClass) {
			return "root";
		} else if (child instanceof Protocol) {
			return ((Protocol) child).getProtocolClass();
		}
		return null;
	}
	
	@Override
	public boolean hasChildren(Object parent) {
		if (parent instanceof String) {
			return true;
		} else if (parent instanceof ProtocolClass) {
			ProtocolClass pc = (ProtocolClass)parent;
			List<Protocol> protocols = ProtocolService.getInstance().getProtocols(pc);
			return (protocols != null && !protocols.isEmpty());
		}
		return false;
	}
	
	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof String) {
			List<ProtocolClass> pClasses = ProtocolService.getInstance().getProtocolClasses();
			ProtocolClass[] pClassArray = pClasses.toArray(new ProtocolClass[pClasses.size()]);
			Arrays.sort(pClassArray, ProtocolUtils.PROTOCOLCLASS_NAME_SORTER);
			return pClassArray;
		} else if (parent instanceof ProtocolClass) {
			ProtocolClass pc = (ProtocolClass)parent;
			List<Protocol> protocols = ProtocolService.getInstance().getProtocols(pc);
			Protocol[] protocolArray = protocols.toArray(new Protocol[protocols.size()]);
			Arrays.sort(protocolArray, ProtocolUtils.PROTOCOL_NAME_SORTER);
			return protocolArray;
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}

}
