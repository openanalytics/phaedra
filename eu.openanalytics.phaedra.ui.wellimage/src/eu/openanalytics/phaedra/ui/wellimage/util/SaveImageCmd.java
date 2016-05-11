package eu.openanalytics.phaedra.ui.wellimage.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.ui.wellimage.Activator;

public class SaveImageCmd {

	public void execute(String fileName, Image image, JP2KOverlay overlay) {
		
		if (fileName == null || image == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "No image", "Cannot save: no image available.");
			return;
		}
		
		FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		dialog.setFileName(fileName);
		dialog.setFilterExtensions(new String[]{"*.png"});
		final String destination = dialog.open();

		if (destination == null) return;
		
		try {
			ImageLoader imgLoader = new ImageLoader();
			imgLoader.data = new ImageData[] { mergeOverlay(image, overlay) };
			imgLoader.save(destination, SWT.IMAGE_PNG);
		} catch (Throwable t) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save failed", "Cannot save image: " + t.getMessage());
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Save image failed", t));
		}
	}
	
	private ImageData mergeOverlay(Image image, JP2KOverlay overlay) {
		if (overlay == null) return image.getImageData();
		Image tmpImage = null;
		GC gc = null;
		try {
			tmpImage = new Image(null, image, SWT.IMAGE_COPY);
			gc = new GC(tmpImage);
			overlay.render(gc);
			ImageData data = tmpImage.getImageData();
			return data;
		} finally {
			if (gc != null) gc.dispose();
			if (tmpImage != null) tmpImage.dispose();
		}
	}
}
