package eu.openanalytics.phaedra.base.ui.navigator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;

public class NavigatorLabelProvider extends StyledCellLabelProvider {

	private final static Image ICON_GROUP = IconManager.getIconImage("folder.png");
	private final static Image ICON_ELEMENT = IconManager.getIconImage("file.png");
	
	private LocalResourceManager iconMgr;
	
	public NavigatorLabelProvider() {
		iconMgr = new LocalResourceManager(JFaceResources.getResources());		 
	}
	
	@Override
	public void dispose() {
		iconMgr.dispose();
		super.dispose();
	}
	
	@Override
	public void update(final ViewerCell cell) {
		if (!(cell.getElement() instanceof IElement)) return;
		
		IElement element = (IElement)cell.getElement();
		
		StyledString styledString = new StyledString(element.getName(), null);
		
		String[] decorations = element.getDecorations();
		if (decorations != null) {
			Styler[] styles = {StyledString.DECORATIONS_STYLER, StyledString.COUNTER_STYLER, StyledString.QUALIFIER_STYLER };
			for (int i=0; i<decorations.length; i++) {
				if (decorations[i] == null) continue;
				Styler decorationStyler = styles[i%styles.length];
				styledString.append(" " + decorations[i], decorationStyler);				
			}
		}
		
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());

		ImageDescriptor imgDesc = element.getImageDescriptor();
		if (imgDesc != null) {
			cell.setImage((Image)iconMgr.get(imgDesc));
		} else {
			if (element instanceof IGroup) {
				cell.setImage(ICON_GROUP);
			} else {
				cell.setImage(ICON_ELEMENT);
			}
		}
	}
	
	@Override
	public String getToolTipText(Object element) {
		return ((IElement) element).getTooltip();
	}
}
