package eu.openanalytics.phaedra.base.ui.search.navigator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;

import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.navigator.interaction.BaseElementHandler;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;

public class QueryHandler extends BaseElementHandler {

	@Override
	public boolean matches(IElement element) {
		return (element.getId().startsWith(QueryProvider.PUBLIC_QUERIES + "-") 
				|| element.getId().startsWith(QueryProvider.MY_QUERIES + "-")
				|| element.getId().startsWith(QueryProvider.EXAMPLE_QUERIES + "-"));
	}

	@Override
	public void handleDoubleClick(IElement element) {
		QueryModel queryModel = (QueryModel) element.getData();
		EditorFactory.getInstance().openEditor(queryModel);
	}
	
	@Override
	public void dragStart(IElement element, DragSourceEvent event) {
		event.doit = true;
		event.detail = DND.DROP_COPY;
	}

	@Override
	public void dragSetData(IElement element, DragSourceEvent event) {
		Object data = element.getData();
		LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(data));
	}
}