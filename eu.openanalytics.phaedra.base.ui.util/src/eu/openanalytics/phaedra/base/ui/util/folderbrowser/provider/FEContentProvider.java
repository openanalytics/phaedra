package eu.openanalytics.phaedra.base.ui.util.folderbrowser.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.sf.feeling.swt.win32.extension.shell.ShellLink;

public class FEContentProvider implements ITreeContentProvider {
	
	private boolean showFiles;
	
	public FEContentProvider(boolean showFiles) {
		this.showFiles = showFiles;
	}
	
	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// Do nothing.
	}
	
	@Override
	public void dispose() {
		// Nothing to dispose.
	}
	
	@Override
	public Object[] getElements(Object input) {		
		return getChildren(input);
	}
	
	@Override
	public Object getParent(Object child) {
		if (child instanceof File) return ((File) child).getParentFile();
		return null;
	}
	
	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof TreeRoot) {
			return ((TreeRoot) parent).getChildren();
		} else {
			File file = (File) parent;
			if (file.isDirectory()) {
				// Attempt to resolve links. Doesn't work for unc root paths.
				if (file.getName().endsWith(".lnk")) {
					String target = ShellLink.getShortCutTarget(file);
					if (target != null && !target.isEmpty()) {
						file = new File(target);
					}
				}

				File[] files = file.listFiles();
				List<File> children = new ArrayList<>();
				for (File item: files) {
					if (item.isDirectory()) {
						children.add(item);
					} else if (showFiles) {
						children.add(item);
					}
				}
				return children.toArray(new File[children.size()]);
			} else {
				return new Object[0];
			}
		}
	}
	
	@Override
	public boolean hasChildren(Object parent) {
		if (parent instanceof TreeRoot) return true;
		if (parent instanceof File) return ((File) parent).isDirectory();
		return false;
	}
}
