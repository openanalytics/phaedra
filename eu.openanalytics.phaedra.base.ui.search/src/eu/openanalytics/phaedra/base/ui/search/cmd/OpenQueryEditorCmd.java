package eu.openanalytics.phaedra.base.ui.search.cmd;

import java.util.Date;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import eu.openanalytics.phaedra.base.search.QueryNameGenerator;
import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;

public class OpenQueryEditorCmd extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		QueryModel queryModel = new QueryModel();
		queryModel.setName(QueryNameGenerator.generateQueryName());
		queryModel.setOwner(SecurityService.getInstance().getCurrentUserName());
		queryModel.setDate(new Date());
		
		EditorFactory.getInstance().openEditor(queryModel);
		return null;		
	}
}
