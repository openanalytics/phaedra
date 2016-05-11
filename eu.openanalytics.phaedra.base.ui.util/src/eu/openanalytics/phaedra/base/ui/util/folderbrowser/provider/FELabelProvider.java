package eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.sf.feeling.swt.win32.extension.io.FileSystem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class FELabelProvider extends LabelProvider {
	
	private static final String DESKTOP     = "explorer/desktop.png";
	private static final String MYCOMPUTER  = "explorer/computer.png";
	private static final String NETWORK     = "explorer/mynetwork.png";
	private static final String DRIVE       = "explorer/drive_local.png";
	private static final String MDRIVE      = "explorer/drive_mount.png";
	private static final String UDRIVE      = "explorer/drive_unmount.png";
	private static final String CDROM       = "explorer/cdrom.png";
	private static final String FLOPPY      = "explorer/floppy.png";
	private static final String FOLDER      = "explorer/folder.png";
	private static final String FILE        = "explorer/file.png";
	
	private Pattern patDrive = Pattern.compile("^[[A-Z]:\\\\]+$");
	
	public String getText(Object object) {
		if (object instanceof File) {
			String newName = FileSystemView.getFileSystemView().getSystemDisplayName((File) object);
			if (newName.isEmpty()) newName = ExplorerLabels.MYDOC;
			return newName;
		}
		return object.toString();
	}
	
	public Image getImage(Object object) {
		if (object instanceof File) {
			File file = (File) object;
			String fileIcon = "";
			
			if (file.getName().equals("Bureaublad") || file.getName().equals("Desktop")) {
				fileIcon = DESKTOP;
			} else if (file.getName().equals("::{20D04FE0-3AEA-1069-A2D8-08002B30309D}")) {
				fileIcon = MYCOMPUTER;
			} else if (file.getName().equals("::{208D2C60-3AEA-1069-A2D7-08002B30309D}")) {
				fileIcon = NETWORK;
			} else if (patDrive.matcher(file.getAbsolutePath()).matches() && file.getName().length() == 0) {
				try {
					int type = FileSystem.getDriveType(file);
					switch(type) {
						case FileSystem.DRIVE_TYPE_CDROM:
							fileIcon = CDROM; break;
						case FileSystem.DRIVE_TYPE_FIXED:
							fileIcon = DRIVE; break;
						case FileSystem.DRIVE_TYPE_REMOTE:
							fileIcon = MDRIVE; break;
						case FileSystem.DRIVE_TYPE_REMOVABLE:
							fileIcon = FLOPPY; break;
						case FileSystem.DRIVE_TYPE_NOT_EXIST:
							fileIcon = UDRIVE; break;
						default:
							fileIcon = FILE; break;
					}
				} catch (Throwable t) {
					fileIcon = DRIVE;
				}
			} else if (file.isDirectory()) {
				fileIcon = FOLDER;
			} else {
				fileIcon = FILE;
			}
			return IconManager.getIconImage(fileIcon);
		}
		return IconManager.getIconImage(FILE);
	}
}
