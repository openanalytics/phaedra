package eu.openanalytics.phaedra.base.ui.admin.fs.browser;

import java.io.File;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class FSLabelProvider extends StyledCellLabelProvider {

	private static Image folderIcon = IconManager.getIconImage("folder.png");
	private static Image fileIcon = IconManager.getIconImage("file.png");
	
	@Override
	public void update(ViewerCell cell) {
		File file = (File)cell.getElement();
		
		if (file.isDirectory()) cell.setImage(folderIcon);
		else cell.setImage(fileIcon);
		
		cell.setText(file.getName());
		
		super.update(cell);
	}
}
