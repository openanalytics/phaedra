package eu.openanalytics.phaedra.ui.protocol.cmd;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.util.QualifiedNameGenerator;

public class CopyQualifiedNames extends AbstractHandler {

	private final static String LINE_SEP = System.getProperty("line.separator");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		copyToClipboard(selection);
		return null;
	}

	public static void execute(IValueObject object) {
		if (object != null) {
			copyToClipboard(new StructuredSelection(object));
		}
	}

	private static void copyToClipboard(ISelection selection) {
		if (selection == null || selection.isEmpty()) return;

		StringBuilder sb = new StringBuilder();
		List<IValueObject> objects = SelectionUtils.getObjects(selection, IValueObject.class);
		for (IValueObject object: objects) {
			String qName = QualifiedNameGenerator.getQualifiedName(object);
			if (qName != null) sb.append(qName + LINE_SEP);
		}

		Clipboard clip = new Clipboard(Display.getCurrent());
		Object[] datas = { sb.toString() };
		Transfer[] transfers = { TextTransfer.getInstance() };
		clip.setContents(datas, transfers);
	}
}