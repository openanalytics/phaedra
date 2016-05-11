package eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

public class TreeRoot {

	private File[] children;
	
	public TreeRoot(File... contents) {
		if (contents.length == 0) {
			this.children = new File[] { FileSystemView.getFileSystemView().getHomeDirectory() };
		} else {
			this.children = contents;
		}
	}

	public File[] getChildren() {
		return children;
	}
}
