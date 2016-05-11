package eu.openanalytics.phaedra.base.ui.util.copy.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.ICopyable;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;

public class CopyView extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}

	public static void execute() {
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (activePart == null) return;
		if (activePart instanceof ICopyable) copyToClipboard((ICopyable)activePart);
		else if (activePart instanceof IDecoratedPart) {
			IDecoratedPart view = (IDecoratedPart) activePart;
			CopyableDecorator decorator = view.hasDecorator(CopyableDecorator.class);
			if (decorator != null) copyToClipboard(decorator);
		}
	}

	private static void copyToClipboard(ICopyable copyable) {
		Point size = copyable.getCopySize();

		Clipboard clip = new Clipboard(Display.getCurrent());
		Image image = new Image(null, size.x, size.y);
		GC gc = new GC(image);
		try {
			copyable.copy(gc);
		} finally {
			gc.dispose();
		}
		ImageData imageData = image.getImageData();
		image.dispose();
		Object[] images = { imageData };
		Transfer[] it = { ImageTransfer.getInstance() };
		clip.setContents(images, it);
	}
}
