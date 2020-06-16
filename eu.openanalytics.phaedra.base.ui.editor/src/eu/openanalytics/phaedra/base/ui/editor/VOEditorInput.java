package eu.openanalytics.phaedra.base.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.ui.editor.VOElementFactory.PersistableVOInput;
import eu.openanalytics.phaedra.base.ui.icons.IconRegistry;

public class VOEditorInput implements IEditorInput {

	private List<IValueObject> valueObjects;
	private IDynamicVOCollector collector;
	
	public VOEditorInput(IValueObject valueObject) {
		this.valueObjects = new ArrayList<>();
		this.valueObjects.add(valueObject);
	}
	
	public VOEditorInput(List<? extends IValueObject> valueObjects) {
		this.valueObjects = new ArrayList<>();
		this.valueObjects.addAll(valueObjects);
	}

	public VOEditorInput(IDynamicVOCollector collector) {
		this.collector = collector;
	}
	
	/**
	 * Returns the value objects of the editor input.
	 * <p>
	 * The optional filter allows to prefilter the list of value objects before/while loading.
	 * The filter is a hint, there is no guarantee that the filter is applied!</p>
	 * 
	 * @param filter an optional filter parameters to prefilter the objects
	 * @return a list with the value objects
	 */
	public List<IValueObject> getValueObjects(Map<String, Object> filter) {
		if (collector != null) {
			return collector.collect((filter != null) ? filter : Collections.emptyMap());
		}
		return valueObjects;
	}
	
	/**
	 * Returns the value objects of the editor input.
	 * 
	 * @return a list with the value objects
	 */
	public List<IValueObject> getValueObjects() {
		return getValueObjects(null);
	}
	
	public IDynamicVOCollector getCollector() {
		return collector;
	}
	
	@Override
	public boolean exists() {
		if (collector != null) return true;
		return getValueObjects().stream().noneMatch(vo -> (vo.getId() == 0));
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (collector != null) return collector.getImageDescriptor();
		
		if (valueObjects.isEmpty()) return null;
		return IconRegistry.getInstance().getDefaultImageDescriptorFor(valueObjects.get(0).getClass());
	}

	@Override
	public String getName() {
		if (collector != null) return collector.getEditorName();
		
		if (valueObjects.isEmpty()) return "";
		IValueObject sample = valueObjects.get(0);
		if (valueObjects.size() == 1) return sample.toString();
		String objectName = sample.getClass().getSimpleName();
		if (objectName.endsWith("s")) objectName += "e";
		return valueObjects.size() + " " + objectName + "s";
	}

	@Override
	public IPersistableElement getPersistable() {
		if (!exists()) return null;
		return new PersistableVOInput(this);
	}

	@Override
	public String getToolTipText() {
		return getName();
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public int hashCode() {
		if (collector != null) return collector.hashCode();
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((valueObjects == null) ? 0 : valueObjects.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		VOEditorInput other = (VOEditorInput) obj;
		
		if (collector != null && other.collector != null) return collector.equals(other.collector);
		
		if (valueObjects == null) {
			if (other.valueObjects != null) return false;
		} else if (!valueObjects.equals(other.valueObjects))
			return false;
		return true;
	}
}
