package eu.openanalytics.phaedra.base.ui.volumerenderer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;


public class VolumeViewEditorInput implements IEditorInput {

	private VolumeDataModel dataModel;

	public VolumeViewEditorInput(VolumeDataModel dataModel) {
		this.dataModel = dataModel;
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
	public String getName() {
		return "Volume View";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	public VolumeDataModel getDataModel() {
		return dataModel;
	}

	public void setDataModel(VolumeDataModel dataModel) {
		this.dataModel = dataModel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dataModel == null) ? 0 : dataModel.hashCode());
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
		VolumeViewEditorInput other = (VolumeViewEditorInput) obj;
		if (dataModel == null) {
			if (other.dataModel != null)
				return false;
		} else if (!dataModel.equals(other.dataModel))
			return false;
		return true;
	}

}
