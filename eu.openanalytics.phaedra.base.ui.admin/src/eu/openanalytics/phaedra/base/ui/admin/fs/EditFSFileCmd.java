package eu.openanalytics.phaedra.base.ui.admin.fs;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.admin.fs.editor.FSPathEditorInput;
import eu.openanalytics.phaedra.base.ui.admin.fs.editor.SimpleEditor;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class EditFSFileCmd extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection)HandlerUtil.getCurrentSelection(event);
		String fsRelativePath = SelectionUtils.getFirstObject(selection, String.class);
		execute(fsRelativePath);
		return null;
	}
	
	public static void execute(String fsRelativePath) {
		if (fsRelativePath == null || fsRelativePath.isEmpty()) return;
		IPath location= new Path(fsRelativePath);
		FSPathEditorInput input= new FSPathEditorInput(location);
		String editorId = SimpleEditor.class.getName();
		IWorkbenchPage page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			page.openEditor(input, editorId);
		} catch (PartInitException ex) {}
	}
}
