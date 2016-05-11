package eu.openanalytics.phaedra.base.ui.editor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.ui.IEditorInput;

import eu.openanalytics.phaedra.base.db.IValueObject;

public class VOEditorInputFactory implements IEditorInputFactory {

	@Override
	public IEditorInput createEditorInput(Object input) {
		if (input instanceof IValueObject) return new VOEditorInput((IValueObject)input);
		if (input instanceof Collection) {
			Collection<?> inputList = (Collection<?>)input;
			if (inputList.isEmpty()) return null;
			List<IValueObject> voList = inputList.stream()
					.filter(o -> o instanceof IValueObject).map(o -> (IValueObject)o).collect(Collectors.toList());
			return new VOEditorInput(voList);
		}
		return null;
	}

	@Override
	public String getEditorIdFor(Object input) {
		return null;
	}
}
