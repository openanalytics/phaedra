package eu.openanalytics.phaedra.ui.protocol.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassEditorInput implements IEditorInput {

	private ProtocolClass protocolClass;
	private ProtocolClass originalProtocolClass;
	
	private boolean newProtocolClass = false;
	
	public ProtocolClassEditorInput(ProtocolClass protocolClass, boolean isNew) {
		this.originalProtocolClass = protocolClass;
		this.protocolClass = ProtocolService.getInstance().createProtocolClass();
		ObjectCopyFactory.copySettings(originalProtocolClass, this.protocolClass);
		newProtocolClass = isNew;
	}

	@Override
	public String getName() {
		return protocolClass.getName();
	}

	public ProtocolClass getProtocolClass() {
		return this.protocolClass;
	}
	
	public ProtocolClass getOriginalProtocolClass() {
		return originalProtocolClass;
	}
	
	public boolean isNewProtocolClass() {
		return newProtocolClass;
	}
	
	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ProtocolClass.class) return adapter.cast(getProtocolClass());
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((protocolClass == null) ? 0 : protocolClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		if (newProtocolClass) return false;
		
		ProtocolClassEditorInput other = (ProtocolClassEditorInput) obj;
		if (protocolClass == null) {
			if (other.protocolClass != null) return false;
		} else if (!protocolClass.equals(other.protocolClass)) return false;
		return true;
	}
}
