package eu.openanalytics.phaedra.base.ui.navigator.model;

public interface IGroup extends IElement {

	public String getId();
	
	public IElement[] getChildren();
	
	public boolean isStartExpanded();
}
