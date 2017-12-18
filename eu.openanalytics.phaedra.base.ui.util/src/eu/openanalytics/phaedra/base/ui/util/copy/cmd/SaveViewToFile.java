package eu.openanalytics.phaedra.base.ui.util.copy.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.copy.ICopyable;
import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;

public class SaveViewToFile extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		execute();
		return null;
	}
	
	public static void execute() {
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (activePart == null) return;
		if (activePart instanceof ICopyable) saveClipToFile((ICopyable)activePart);
		else if (activePart instanceof IDecoratedPart) {
			IDecoratedPart view = (IDecoratedPart) activePart;
			CopyableDecorator decorator = view.hasDecorator(CopyableDecorator.class);
			if (decorator != null) saveClipToFile(decorator);
		}
	}
	
	private static void saveClipToFile(ICopyable copyable) {
		Point size = copyable.getCopySize();

		Image image = new Image(null, size.x, size.y);
		GC gc = new GC(image);
		try {
			copyable.copy(gc);
		} finally {
			gc.dispose();
		}
		ImageData imageData = image.getImageData();
		image.dispose();
		
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setFileName("screenshot");
		dialog.setFilterExtensions(new String[] { "*.png" });
		String destination = dialog.open();
		if (destination == null) return;
		
		ImageLoader il = new ImageLoader();
		il.data = new ImageData[] { imageData };
		il.save(destination, SWT.IMAGE_PNG);
	}
}
