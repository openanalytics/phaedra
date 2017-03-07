package eu.openanalytics.phaedra.base.ui.util.copy.cmd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.openanalytics.phaedra.base.ui.util.Activator;
import eu.openanalytics.phaedra.base.ui.util.copy.extension.ICopyTextProvider;

/**
 * <p>
 * Copy items to the system clipboard.
 * </p><p>
 * The data is copied in two formats:
 * <ul>
 * <li>The objects are copied via <code>LocalSelectionTransfer</code>, so they can be used by other parts of Phaedra</li>
 * <li>A text representation of the objects is copied via <code>TextTransfer</code>, so they can be pasted in other applications</li>
 * </ul>
 * </p>
 */
public class CopyItems extends AbstractHandler {

	public final static String COPY_PASTE_CONTEXT_ID = Activator.PLUGIN_ID + ".CopyPasteContext";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		copyToClipboard(selection, event);
		return null;
	}

	public static void execute(ISelection selection) {
		copyToClipboard(selection, null);
	}

	public static void execute(String text) {
		TextTransfer textTransfer = TextTransfer.getInstance();
		Object[] datas = { text };
		Transfer[] transfers = { textTransfer };
		setClipboardContent(datas, transfers);
	}
	
	public static ISelection getCurrentSelectionFromClipboard() {
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			TransferData[] transfers = cb.getAvailableTypes();
			for (TransferData transferData: transfers) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferData)) {
					ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
					if (selection != null) return selection;
				}
			}
		} finally {
			cb.dispose();
		}
		return null;
	}

	public static String getCurrentTextFromClipboard() {
		String output;
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			Object contents = cb.getContents(TextTransfer.getInstance());
			if (contents instanceof String) {
				output = (String) contents;
			} else {
				output = "";
			}
		} finally {
			cb.dispose();
		}
		return output;
	}

	private static void copyToClipboard(ISelection selection, ExecutionEvent event) {
		if (selection == null || selection.isEmpty()) return;

		Object value = getSelectionValue(selection, event);
		Transfer valueTransfer = null;
		if (value instanceof String) valueTransfer = TextTransfer.getInstance();
		else if (value instanceof ImageData) valueTransfer = ImageTransfer.getInstance();
		
		Object[] datas = null;
		Transfer[] transfers = null;
		if (valueTransfer == null) {
			datas = new Object[] { selection };
			transfers = new Transfer[] { getLocalSelectionTransfer(selection) };
		} else {
			datas = new Object[] { selection, value };
			transfers = new Transfer[] { getLocalSelectionTransfer(selection), valueTransfer };
		}

		setClipboardContent(datas, transfers);
	}

	private static Object getSelectionValue(ISelection selection, ExecutionEvent event) {
		if (event != null) {
			Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
			if (control != null) {
				List<ICopyTextProvider> copyTextProviders = loadCopyTextProviders();
				for (ICopyTextProvider ctp : copyTextProviders) {
					if (ctp.isValidWidget(control)) {
						return ctp.getValueToCopy(control);
					}
				}
			}
		}
		
		if (selection instanceof StructuredSelection) {
			StringBuilder sb = new StringBuilder();
			Iterator<?> it = ((StructuredSelection)selection).iterator();
			String lineSep = System.getProperty("line.separator");
			while (it.hasNext()) {
				sb.append(it.next().toString() + lineSep);
			}
			return sb.toString();
		}
		
		return selection.toString();
	}

	private static LocalSelectionTransfer getLocalSelectionTransfer(ISelection selection) {
		LocalSelectionTransfer selectionTransfer = LocalSelectionTransfer.getTransfer();
		selectionTransfer.setSelection(selection);
		selectionTransfer.setSelectionSetTime(System.currentTimeMillis());
		return selectionTransfer;
	}

	private static List<ICopyTextProvider> loadCopyTextProviders() {
		List<ICopyTextProvider> providers = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICopyTextProvider.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(ICopyTextProvider.ATTR_CLASS);
				if (o instanceof ICopyTextProvider) providers.add((ICopyTextProvider) o);
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		return providers;
	}

	/**
	 * @see {@link Clipboard#setContents(Object[], Transfer[])}
	 */
	private static void setClipboardContent(Object[] datas, Transfer[] transfers) {
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			cb.setContents(datas, transfers);
		} finally {
			cb.dispose();
		}
	}

}