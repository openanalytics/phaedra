package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class WellImageEditorInput implements IEditorInput {

	private Plate plate;
	
	public WellImageEditorInput(Plate plate) {
		this.plate = plate;
	}
	
	public Plate getPlate() {
		return plate;
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
		return "" + plate.getBarcode();
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
		return plate.getAdapter(adapter);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((plate == null) ? 0 : plate.hashCode());
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
		WellImageEditorInput other = (WellImageEditorInput) obj;
		if (plate == null) {
			if (other.plate != null)
				return false;
		} else if (!plate.equals(other.plate))
			return false;
		return true;
	}

}
