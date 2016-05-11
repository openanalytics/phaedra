package eu.openanalytics.phaedra.base.ui.util.view;

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ShowSecondaryViewDecorator extends PartDecorator {

	@Override
	public void contributeContextMenu(IMenuManager manager) {
		ImageDescriptor img = new ImageDescriptor() {
			@Override
			public ImageData getImageData() {
				return getWorkBenchPart().getTitleImage().getImageData();
			}
		};
		Action action = new Action("New " + getWorkBenchPart().getTitle(), img) {
			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				String secId = UUID.randomUUID().toString();
				try {
					page.showView(getWorkBenchPart().getClass().getName(), secId, IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e) {}
			}
		};
		manager.add(action);
		
	}
	
}