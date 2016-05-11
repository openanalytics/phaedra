package eu.openanalytics.phaedra.base.ui.navigator.model;

import org.eclipse.jface.resource.ImageDescriptor;

public class Group extends Element implements IGroup {

	private IElement[] children;
	private boolean startExpanded;
	
	public Group() {
		// Default constructor.
	}
	
	public Group(String name, String id, String parentId) {
		this(name, id, parentId, false, null);
	}
	
	public Group(String name, String id, String parentId, ImageDescriptor imgDesc) {
		this(name, id, parentId, false, imgDesc);
	}
	
	public Group(String name, String id, String parentId, boolean startExpanded, ImageDescriptor imgDesc) {
		super(name, id, parentId, imgDesc);
		this.startExpanded = startExpanded;
	}
	
	@Override
	public IElement[] getChildren() {
		return children;
	}
	
	@Override
	public boolean isStartExpanded() {
		return startExpanded;
	}
}
