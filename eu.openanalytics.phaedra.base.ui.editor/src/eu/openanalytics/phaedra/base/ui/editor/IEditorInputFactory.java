package eu.openanalytics.phaedra.base.ui.editor;

import org.eclipse.ui.IEditorInput;

public interface IEditorInputFactory {

	public IEditorInput createEditorInput(Object input);

	public String getEditorIdFor(Object input);
}
