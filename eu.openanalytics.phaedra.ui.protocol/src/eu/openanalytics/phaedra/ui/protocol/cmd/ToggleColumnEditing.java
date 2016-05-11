package eu.openanalytics.phaedra.ui.protocol.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnEditingFactory;

public class ToggleColumnEditing extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Event trigger = (Event) event.getTrigger();
		boolean enabled = ((ToolItem) trigger.widget).getSelection();
		ColumnEditingFactory.toggleColumnEditing(enabled);
		return null;
	}

}
