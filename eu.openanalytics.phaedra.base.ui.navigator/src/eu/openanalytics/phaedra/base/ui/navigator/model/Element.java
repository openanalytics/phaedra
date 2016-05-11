package eu.openanalytics.phaedra.base.ui.navigator.model;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;

public class Element extends PlatformObject implements IElement {

	private String name;
	private String id;
	private String parentId;
	private IGroup parent;
	private String tooltip;
	private String[] decorations;

	private ImageDescriptor imageDescriptor;

	private Object data;

	public Element() {
		// Default constructor
	}

	public Element(String name, String id, String parentId) {
		this(name, id, parentId, null);
	}

	public Element(String name, String id, String parentId, ImageDescriptor imgDesc) {
		this(name, id, parentId, null, imgDesc);
	}

	public Element(String name, String id, String parentId, String tooltip, ImageDescriptor imgDesc) {
		this.name = name;
		this.id = id;
		this.parentId = parentId;
		this.tooltip = tooltip;
		this.imageDescriptor = imgDesc;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	@Override
	public IGroup getParent() {
		return parent;
	}

	public void setParent(IGroup parent) {
		this.parent = parent;
	}

	@Override
	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	@Override
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return imageDescriptor;
	}

	public void setImageDescriptor(ImageDescriptor imageDescriptor) {
		this.imageDescriptor = imageDescriptor;
	}

	@Override
	public String[] getDecorations() {
		return decorations;
	}

	public void setDecorations(String[] decorations) {
		this.decorations = decorations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		Element other = (Element) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (data != null && adapter.isAssignableFrom(data.getClass())) return data;
		return super.getAdapter(adapter);
	}
}
